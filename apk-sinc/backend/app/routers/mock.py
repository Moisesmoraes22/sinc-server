from fastapi import APIRouter, Depends, HTTPException

from app.config import get_settings
from app.models.schemas import MockScenarioRequest
from app.monitor.factory import get_mock_client
from app.security import require_api_key

router = APIRouter(
    prefix="/api/mock",
    tags=["mock"],
    dependencies=[Depends(require_api_key)],
)


@router.post("/scenario")
def set_scenario(payload: MockScenarioRequest) -> dict:
    settings = get_settings()
    if settings.source_mode != "mock":
        raise HTTPException(status_code=400, detail="SOURCE_MODE nao esta em 'mock'")
    get_mock_client().set_scenario(payload.a7_code, payload.sequence)
    return {"status": "ok"}


@router.post("/reset")
def reset_scenarios() -> dict:
    get_mock_client().clear_scenarios()
    return {"status": "ok"}
