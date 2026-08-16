import pytest

from app.database.repository import EventRepository, ServerRepository
from app.monitor.mock_source import MockSourceClient
from app.notifications.fcm_client import FcmClient
from app.services.monitor_service import MonitorService


class RecordingFcmClient(FcmClient):
    def __init__(self):
        self.sent: list[tuple[str, str, str]] = []

    def send_status_change(self, server_id, server_name, new_status, reason=None):
        self.sent.append((server_id, server_name, new_status))
        return True


@pytest.mark.asyncio
async def test_three_consecutive_failures_trigger_single_offline_notification(db_session):
    mock = MockSourceClient(servers=[{"id": "s1", "name": "Servidor 1"}])
    mock.set_scenario("s1", ["down", "down", "down", "down"])
    fcm = RecordingFcmClient()
    service = MonitorService(mock, fcm)

    for _ in range(4):
        await service.run_check_cycle(db_session)

    server = ServerRepository(db_session).get("s1")
    assert server.status == "OFFLINE"
    assert fcm.sent == [("s1", "Servidor 1", "OFFLINE")]


@pytest.mark.asyncio
async def test_recovery_after_offline_notifies_and_updates_history(db_session):
    mock = MockSourceClient(servers=[{"id": "s1", "name": "Servidor 1"}])
    mock.set_scenario("s1", ["down", "down", "down", "up"])
    fcm = RecordingFcmClient()
    service = MonitorService(mock, fcm)

    for _ in range(4):
        await service.run_check_cycle(db_session)

    server = ServerRepository(db_session).get("s1")
    assert server.status == "ONLINE"
    assert fcm.sent == [("s1", "Servidor 1", "OFFLINE"), ("s1", "Servidor 1", "ONLINE")]

    events = EventRepository(db_session).list_all(server_id="s1")
    to_statuses = [e.to_status for e in reversed(events)]
    assert to_statuses == ["ATENCAO", "OFFLINE", "ONLINE"]


@pytest.mark.asyncio
async def test_no_notification_for_single_transient_failure(db_session):
    mock = MockSourceClient(servers=[{"id": "s1", "name": "Servidor 1"}])
    mock.set_scenario("s1", ["down", "up"])
    fcm = RecordingFcmClient()
    service = MonitorService(mock, fcm)

    await service.run_check_cycle(db_session)
    await service.run_check_cycle(db_session)

    assert fcm.sent == []
    server = ServerRepository(db_session).get("s1")
    assert server.status == "ONLINE"
    assert server.consecutive_failures == 0


@pytest.mark.asyncio
async def test_down_count_increments_only_once_per_outage(db_session):
    mock = MockSourceClient(servers=[{"id": "s1", "name": "Servidor 1"}])
    mock.set_scenario("s1", ["down", "down", "down", "down", "down", "up", "down", "down", "down"])
    service = MonitorService(mock, RecordingFcmClient())

    for _ in range(9):
        await service.run_check_cycle(db_session)

    server = ServerRepository(db_session).get("s1")
    assert server.down_count == 2


@pytest.mark.asyncio
async def test_source_error_does_not_crash_cycle(db_session):
    class BrokenSource:
        async def fetch_servers(self):
            raise ConnectionError("fonte indisponivel")

    service = MonitorService(BrokenSource(), RecordingFcmClient())
    readings = await service.run_check_cycle(db_session)
    assert readings == []
