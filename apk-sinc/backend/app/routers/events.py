from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.database.db import get_session
from app.database.repository import EventRepository
from app.models.schemas import EventListResponse, EventOut

router = APIRouter(prefix="/api/events", tags=["events"])


def _session() -> Session:
    session = get_session()
    try:
        yield session
    finally:
        session.close()


@router.get("", response_model=EventListResponse)
def list_events(
    server_id: str | None = None,
    status: str | None = Query(default=None, description="Filtrar por status destino: ONLINE, ATENCAO, OFFLINE"),
    limit: int = 200,
    session: Session = Depends(_session),
) -> EventListResponse:
    events = EventRepository(session).list_all(server_id=server_id, status=status, limit=limit)
    return EventListResponse(events=[EventOut.model_validate(e) for e in events])
