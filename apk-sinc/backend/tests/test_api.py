import pytest
from fastapi.testclient import TestClient

from app.dependencies import get_repository
from app.main import app
from app.models.domain import CATEGORY_AGUA, METRIC_WATER_ML, Habit


@pytest.fixture()
def client(repository):
    app.dependency_overrides[get_repository] = lambda: repository
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()


def test_health_ok(client, repository):
    response = client.get("/api/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["database"] == "connected"
    assert body["db_backend"] in ("sqlite", "supabase")


def test_get_profile_creates_default(client):
    response = client.get("/api/profile")
    assert response.status_code == 200
    assert response.json()["display_name"]


def test_update_profile(client):
    updated = client.put("/api/profile", json={"display_name": "Ana Paula"})
    assert updated.status_code == 200
    assert updated.json()["display_name"] == "Ana Paula"

    fetched = client.get("/api/profile").json()
    assert fetched["display_name"] == "Ana Paula"


def test_list_habits_includes_today_state(client, repository):
    profile = repository.get_or_create_profile("Ana")
    repository.upsert_habit(
        Habit(profile_id=profile.id, title="Beber agua", category=CATEGORY_AGUA, target_value=2.5, target_unit="L")
    )

    response = client.get("/api/habits")
    assert response.status_code == 200
    habits = response.json()["habits"]
    assert len(habits) == 1
    assert habits[0]["title"] == "Beber agua"
    assert habits[0]["today_completed"] is False


def test_log_habit_marks_completed(client, repository):
    profile = repository.get_or_create_profile("Ana")
    habit = repository.upsert_habit(Habit(profile_id=profile.id, title="Beber agua", category=CATEGORY_AGUA))

    response = client.post(f"/api/habits/{habit.id}/log", json={"value": 2.5, "completed": True})
    assert response.status_code == 200
    body = response.json()
    assert body["today_completed"] is True
    assert body["today_value"] == 2.5


def test_log_habit_not_found(client):
    response = client.post("/api/habits/nao-existe/log", json={"completed": True})
    assert response.status_code == 404


def test_metrics_create_and_list(client):
    created = client.post("/api/metrics", json={"metric_type": METRIC_WATER_ML, "value": 500})
    assert created.status_code == 200

    listed = client.get("/api/metrics", params={"metric_type": METRIC_WATER_ML}).json()
    assert len(listed["metrics"]) == 1
    assert listed["metrics"][0]["value"] == 500


def test_metrics_create_rejects_invalid_type(client):
    response = client.post("/api/metrics", json={"metric_type": "NAO_EXISTE", "value": 1})
    assert response.status_code == 422


def test_home_summary_reflects_habits_and_metrics(client, repository):
    profile = repository.get_or_create_profile("Ana")
    habit = repository.upsert_habit(Habit(profile_id=profile.id, title="Beber agua", category=CATEGORY_AGUA))
    client.post(f"/api/habits/{habit.id}/log", json={"completed": True})

    response = client.get("/api/home/summary")
    assert response.status_code == 200
    body = response.json()
    assert body["greeting_name"] == "Ana"
    assert body["habits_done_today"] == 1
    assert body["habits_total_today"] == 1
    assert body["next_action_habit_id"] is None  # unico habito ja concluido


def test_register_device_and_list(client):
    response = client.post("/api/devices", json={"token": "abc123", "platform": "android"})
    assert response.status_code == 200

    devices = client.get("/api/devices").json()["devices"]
    assert len(devices) == 1
    assert devices[0]["device_name"] == "android"
    assert "fcm_token" not in devices[0]
