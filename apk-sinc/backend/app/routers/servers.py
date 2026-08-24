"""Endpoints legados (dominio "servers") mantidos para compatibilidade com
o app Android existente. Internamente consultam as sync_units no Supabase/
SQLite e adaptam a resposta - ver app/services/legacy_adapter.py."""

from fastapi import APIRouter, Depends, HTTPException

from app.database.base import Repository
from app.dependencies import get_repository
from app.models.schemas import ServerListResponse, ServerOut
from app.security import require_api_key
from app.services.legacy_adapter import unit_to_server_out

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
