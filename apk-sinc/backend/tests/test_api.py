import pytest
from fastapi.testclient import TestClient

from app.dependencies import get_repository
from app.main import app
from app.models.domain import CRITICO, NORMAL, SyncUnit


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


def test_sync_units_and_legacy_servers_endpoint(client, repository):
    repository.upsert_unit(SyncUnit(a7_code="A7-1", business_unit="Unidade Teste", overall_status=CRITICO))

    sync_response = client.get("/api/sync-units")
    assert sync_response.status_code == 200
    assert sync_response.json()["units"][0]["overall_status"] == CRITICO

    legacy_response = client.get("/api/servers")
    assert legacy_response.status_code == 200
    server = legacy_response.json()["servers"][0]
    assert server["id"] == "A7-1"
    assert server["name"] == "Unidade Teste"
    assert server["status"] == "OFFLINE"  # CRITICO mapeado para o vocabulario legado


def test_server_detail_not_found(client):
    response = client.get("/api/servers/nao-existe")
    assert response.status_code == 404


def test_register_device_and_list(client):
    response = client.post("/api/devices", json={"token": "abc123", "platform": "android"})
    assert response.status_code == 200

    devices = client.get("/api/devices").json()["devices"]
    assert len(devices) == 1
    assert devices[0]["device_name"] == "android"
    assert "fcm_token" not in devices[0]


def test_events_endpoint(client, repository):
    from app.models.domain import SyncEvent

    unit = repository.upsert_unit(SyncUnit(a7_code="A7-1", business_unit="Unidade Teste"))
    repository.add_event(
        SyncEvent(
            unit_id=unit.id,
            a7_code="A7-1",
            business_unit="Unidade Teste",
            event_type="PROBLEM_STARTED",
            direction="SEND",
            previous_status=NORMAL,
            new_status=CRITICO,
        )
    )
    response = client.get("/api/events")
    assert response.status_code == 200
    events = response.json()["events"]
    assert len(events) == 1
    assert events[0]["from_status"] == "ONLINE"
    assert events[0]["to_status"] == "OFFLINE"
