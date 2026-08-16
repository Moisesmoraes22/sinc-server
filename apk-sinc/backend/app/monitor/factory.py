from app.config import get_settings
from app.monitor.mock_source import MockSourceClient
from app.monitor.real_source import RealSourceClient
from app.monitor.source_client import SourceClient

_mock_singleton: MockSourceClient | None = None


def get_mock_client() -> MockSourceClient:
    global _mock_singleton
    if _mock_singleton is None:
        _mock_singleton = MockSourceClient()
    return _mock_singleton


def build_source_client() -> SourceClient:
    settings = get_settings()
    if settings.source_mode == "real":
        return RealSourceClient()
    return get_mock_client()
