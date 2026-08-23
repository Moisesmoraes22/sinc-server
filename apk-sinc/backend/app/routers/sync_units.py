from fastapi import APIRouter, Depends, HTTPException

from app.database.base import Repository
from app.dependencies import get_repository
from app.models.schemas import SyncUnitListResponse, SyncUnitOut

router = APIRouter(prefix="/api/sync-units", tags=["sync-units"])


@router.get("", response_model=SyncUnitListResponse)
def list_sync_units(repository: Repository = Depends(get_repository)) -> SyncUnitListResponse:
    units = repository.list_units()
    return SyncUnitListResponse(units=[SyncUnitOut.model_validate(u) for u in units])


@router.get("/{a7_code}", response_model=SyncUnitOut)
def get_sync_unit(a7_code: str, repository: Repository = Depends(get_repository)) -> SyncUnitOut:
    unit = repository.get_unit(a7_code)
    if unit is None:
        raise HTTPException(status_code=404, detail="Unidade nao encontrada")
    return SyncUnitOut.model_validate(unit)
