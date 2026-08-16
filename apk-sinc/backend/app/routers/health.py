from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.config import get_settings
from app.database.db import get_session
from app.database.repository import ServerRepository
from app.models.schemas import HealthResponse

router = APIRouter(prefix="/api/health", tags=["health"])


def _session() -> Session:
    session = get_session()
    try:
        yield session
    finally:
        session.close()


@router.get("", response_model=HealthResponse)
def health(session: Session = Depends(_session)) -> HealthResponse:
    from app.main import app_state

    settings = get_settings()
    servers = ServerRepository(session).list_all()
    poller = app_state.get("poller")
    last_poll_at = poller.last_poll_at if poller else None
    return HealthResponse(
        status="ok",
        source_mode=settings.source_mode,
        last_poll_at=last_poll_at,
        servers_count=len(servers),
    )
