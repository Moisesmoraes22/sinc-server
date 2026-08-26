"""Endpoints legados (dominio "servers") mantidos para compatibilidade com
o app Android existente. Internamente consultam as sync_units no Supabase/
SQLite e adaptam a resposta - ver app/services/legacy_adapter.py."""

from fastapi import APIRouter, Depends, HTTPException, Query

from app.database.base import Repository
from app.dependencies import get_repository
from app.models.schemas import ServerListResponse, ServerOut, UptimeOut
from app.security import require_api_key
from app.services.legacy_adapter import unit_to_server_out
from app.services.uptime import compute_uptime

router = APIRouter(
    prefix="/api/servers",
    tags=["servers (legacy)"],
    dependencies=[Depends(require_api_key)],
)


@router.get("", response_model=ServerListResponse)
def list_servers(repository: Repository = Depends(get_repository)) -> ServerListResponse:
    units = repository.list_units()
    settings = repository.get_settings()
    return ServerListResponse(
        servers=[
            unit_to_server_out(
                u,
                warning_threshold_minutes=settings.warning_threshold_minutes,
                critical_threshold_minutes=settings.critical_threshold_minutes,
            )
            for u in units
        ]
    )


@router.get("/{server_id}", response_model=ServerOut)
def get_server(server_id: str, repository: Repository = Depends(get_repository)) -> ServerOut:
    unit = repository.get_unit(server_id)
    if unit is None:
        raise HTTPException(status_code=404, detail="Servidor nao encontrado")
    settings = repository.get_settings()
    return unit_to_server_out(
        unit,
        repository=repository,
        warning_threshold_minutes=settings.warning_threshold_minutes,
        critical_threshold_minutes=settings.critical_threshold_minutes,
    )


@router.get("/{server_id}/uptime", response_model=UptimeOut)
def get_server_uptime(
    server_id: str,
    days: int = Query(default=7, ge=1, le=30),
    repository: Repository = Depends(get_repository),
) -> UptimeOut:
    unit = repository.get_unit(server_id)
    if unit is None:
        raise HTTPException(status_code=404, detail="Servidor nao encontrado")
    events = repository.list_events(unit_a7_code=server_id, limit=1000)
    result = compute_uptime(unit, events, days)
    return UptimeOut(
        period_days=result.period_days,
        uptime_percent=result.uptime_percent,
        downtime_seconds=result.downtime_seconds,
    )
