"""Endpoint legado (dominio "servers") mantido para compatibilidade com o
app Android existente."""

from fastapi import APIRouter, Depends, Query

from app.database.base import Repository
from app.dependencies import get_repository
from app.models.schemas import EventListResponse
from app.security import require_api_key
from app.services.legacy_adapter import event_to_event_out

router = APIRouter(
    prefix="/api/events",
    tags=["events (legacy)"],
    dependencies=[Depends(require_api_key)],
)


@router.get("", response_model=EventListResponse)
def list_events(
    server_id: str | None = None,
    status: str | None = Query(default=None, description="Filtrar por status destino legado: ONLINE, ATENCAO, OFFLINE"),
    limit: int = 200,
    repository: Repository = Depends(get_repository),
) -> EventListResponse:
    # `status` chega no vocabulario legado (ONLINE/ATENCAO/OFFLINE); o
    # repository filtra pelo vocabulario novo (NORMAL/ATENCAO/CRITICO).
    new_status = None
    if status:
        for new, legacy in {"NORMAL": "ONLINE", "ATENCAO": "ATENCAO", "CRITICO": "OFFLINE"}.items():
            if legacy == status.upper():
                new_status = new
                break

    events = repository.list_events(unit_a7_code=server_id, status=new_status, limit=limit)
    return EventListResponse(events=[event_to_event_out(e) for e in events])
