from datetime import datetime, timedelta, timezone

from app.models.domain import ATENCAO, CRITICO, NORMAL, MonitorSettings, SyncUnitReading
from app.notifications.fcm_client import FcmClient
from app.services.monitor_service import MonitorService

WARNING = 5
CRITICAL = 15


class RecordingFcmClient(FcmClient):
    def __init__(self):
        self.sent: list[tuple[str, str, str]] = []

    def send_status_change(self, a7_code, business_unit, new_status, device_tokens=None):
        self.sent.append((a7_code, business_unit, new_status))
        return True


class SingleReadingSource:
    """Fonte de teste que devolve uma unica leitura pre-definida por ciclo."""

    def __init__(self):
        self.reading: SyncUnitReading | None = None

    async def fetch_units(self):
        return [self.reading] if self.reading else []


def _reading(a7_code: str, business_unit: str, elapsed_minutes: float, now: datetime) -> SyncUnitReading:
    reference = now - timedelta(minutes=elapsed_minutes)
    return SyncUnitReading(
        a7_code=a7_code,
        business_unit=business_unit,
        revisions_to_send=0,
        last_send_at=reference,
        last_receive_at=reference,
        checked_at=now,
    )


async def _run(service: MonitorService, source: SingleReadingSource, reading: SyncUnitReading):
    source.reading = reading
    await service.run_check_cycle()


async def test_transition_to_critical_notifies_once(repository):
    repository.update_settings(MonitorSettings(WARNING, CRITICAL, 60))
    source = SingleReadingSource()
    fcm = RecordingFcmClient()
    service = MonitorService(source, repository, fcm)
    now = datetime.now(timezone.utc)

    await _run(service, source, _reading("A7-1", "Unidade 1", 20, now))

    unit = repository.get_unit("A7-1")
    assert unit.overall_status == CRITICO
    assert fcm.sent == [("A7-1", "Unidade 1", CRITICO)]

    events = repository.list_events(unit_a7_code="A7-1")
    assert len(events) == 1
    assert events[0].event_type == "PROBLEM_STARTED"
    assert events[0].previous_status == NORMAL
    assert events[0].new_status == CRITICO


async def test_repeating_critical_does_not_renotify(repository):
    repository.update_settings(MonitorSettings(WARNING, CRITICAL, 60))
    source = SingleReadingSource()
    fcm = RecordingFcmClient()
    service = MonitorService(source, repository, fcm)
    now = datetime.now(timezone.utc)

    await _run(service, source, _reading("A7-1", "Unidade 1", 20, now))
    await _run(service, source, _reading("A7-1", "Unidade 1", 25, now + timedelta(minutes=1)))
    await _run(service, source, _reading("A7-1", "Unidade 1", 30, now + timedelta(minutes=2)))

    assert fcm.sent == [("A7-1", "Unidade 1", CRITICO)]
    events = repository.list_events(unit_a7_code="A7-1")
    assert len(events) == 1


async def test_recovery_notifies_and_computes_duration(repository):
    repository.update_settings(MonitorSettings(WARNING, CRITICAL, 60))
    source = SingleReadingSource()
    fcm = RecordingFcmClient()
    service = MonitorService(source, repository, fcm)
    now = datetime.now(timezone.utc)

    await _run(service, source, _reading("A7-1", "Unidade 1", 20, now))
    later = now + timedelta(minutes=10)
    await _run(service, source, _reading("A7-1", "Unidade 1", 0.5, later))

    unit = repository.get_unit("A7-1")
    assert unit.overall_status == NORMAL
    assert fcm.sent == [("A7-1", "Unidade 1", CRITICO), ("A7-1", "Unidade 1", NORMAL)]

    events = repository.list_events(unit_a7_code="A7-1")
    assert len(events) == 2
    resolved = events[0]
    assert resolved.event_type == "PROBLEM_RESOLVED"
    assert resolved.duration_seconds is not None
    assert resolved.duration_seconds > 0


async def test_attention_transition_notifies_but_stays_below_critical(repository):
    repository.update_settings(MonitorSettings(WARNING, CRITICAL, 60))
    source = SingleReadingSource()
    fcm = RecordingFcmClient()
    service = MonitorService(source, repository, fcm)
    now = datetime.now(timezone.utc)

    await _run(service, source, _reading("A7-1", "Unidade 1", 8, now))

    unit = repository.get_unit("A7-1")
    assert unit.overall_status == ATENCAO
    assert fcm.sent == [("A7-1", "Unidade 1", ATENCAO)]


async def test_send_and_receive_evaluated_independently(repository):
    repository.update_settings(MonitorSettings(WARNING, CRITICAL, 60))
    source = SingleReadingSource()
    service = MonitorService(source, repository, RecordingFcmClient())
    now = datetime.now(timezone.utc)

    reading = SyncUnitReading(
        a7_code="A7-1",
        business_unit="Unidade 1",
        last_send_at=now - timedelta(minutes=20),
        last_receive_at=now - timedelta(minutes=1),
        checked_at=now,
    )
    await _run(service, source, reading)

    unit = repository.get_unit("A7-1")
    assert unit.send_status == CRITICO
    assert unit.receive_status == NORMAL
    assert unit.overall_status == CRITICO


async def test_source_error_does_not_crash_cycle(repository):
    class BrokenSource:
        async def fetch_units(self):
            raise ConnectionError("fonte indisponivel")

    service = MonitorService(BrokenSource(), repository, RecordingFcmClient())
    readings = await service.run_check_cycle()
    assert readings == []
