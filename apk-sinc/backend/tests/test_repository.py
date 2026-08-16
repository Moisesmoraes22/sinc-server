from datetime import datetime, timezone

from app.models.domain import CRITICO, NORMAL, MonitorSettings, SyncEvent, SyncUnit


def test_upsert_unit_creates_and_updates(repository):
    unit = SyncUnit(a7_code="A7-1", business_unit="Unidade Teste", overall_status=NORMAL)
    saved = repository.upsert_unit(unit)
    assert saved.id is not None
    assert saved.a7_code == "A7-1"

    saved.overall_status = CRITICO
    updated = repository.upsert_unit(saved)
    assert updated.overall_status == CRITICO
    assert updated.id == saved.id

    fetched = repository.get_unit("A7-1")
    assert fetched.overall_status == CRITICO


def test_get_unit_not_found_returns_none(repository):
    assert repository.get_unit("nao-existe") is None


def test_list_units_orders_by_business_unit(repository):
    repository.upsert_unit(SyncUnit(a7_code="A7-2", business_unit="Zebra"))
    repository.upsert_unit(SyncUnit(a7_code="A7-1", business_unit="Alfa"))
    units = repository.list_units()
    assert [u.business_unit for u in units] == ["Alfa", "Zebra"]


def test_add_and_list_events(repository):
    unit = repository.upsert_unit(SyncUnit(a7_code="A7-1", business_unit="Unidade Teste"))
    event = SyncEvent(
        unit_id=unit.id,
        a7_code="A7-1",
        business_unit="Unidade Teste",
        event_type="PROBLEM_STARTED",
        direction="SEND",
        previous_status=NORMAL,
        new_status=CRITICO,
        started_at=datetime.now(timezone.utc),
    )
    saved_event = repository.add_event(event)
    assert saved_event.id is not None

    events = repository.list_events(unit_a7_code="A7-1")
    assert len(events) == 1
    assert events[0].new_status == CRITICO

    filtered = repository.list_events(status=NORMAL)
    assert filtered == []


def test_get_last_event_for_unit(repository):
    unit = repository.upsert_unit(SyncUnit(a7_code="A7-1", business_unit="Unidade Teste"))
    repository.add_event(
        SyncEvent(unit_id=unit.id, a7_code="A7-1", business_unit="U", event_type="PROBLEM_STARTED", direction="SEND", new_status=CRITICO)
    )
    last = repository.get_last_event_for_unit("A7-1")
    assert last is not None
    assert last.new_status == CRITICO
    assert repository.get_last_event_for_unit("nao-existe") is None


def test_register_device_is_idempotent(repository):
    first = repository.register_device("token-abc", device_name="android")
    second = repository.register_device("token-abc", device_name="android")
    assert first.id == second.id
    assert len(repository.list_devices()) == 1


def test_list_active_device_tokens(repository):
    repository.register_device("token-1")
    repository.register_device("token-2")
    tokens = repository.list_active_device_tokens()
    assert set(tokens) == {"token-1", "token-2"}


def test_settings_defaults_and_update(repository):
    settings = repository.get_settings()
    assert settings.warning_threshold_minutes == 5
    assert settings.critical_threshold_minutes == 15
    assert settings.check_interval_seconds == 60

    repository.update_settings(MonitorSettings(warning_threshold_minutes=10, critical_threshold_minutes=30, check_interval_seconds=120))
    updated = repository.get_settings()
    assert updated.warning_threshold_minutes == 10
    assert updated.critical_threshold_minutes == 30
    assert updated.check_interval_seconds == 120


def test_health_check_returns_true_when_reachable(repository):
    assert repository.health_check() is True
