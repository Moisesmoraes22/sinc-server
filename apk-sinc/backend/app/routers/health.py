import os

from fastapi import APIRouter, Depends

from app.config import get_settings
from app.database.base import Repository
from app.dependencies import app_state, get_repository
from app.models.schemas import HealthResponse

router = APIRouter(prefix="/api/health", tags=["health"])


@router.get("", response_model=HealthResponse)
def health(repository: Repository = Depends(get_repository)) -> HealthResponse:
    settings = get_settings()

    try:
        database_ok = repository.health_check()
    except Exception:
        database_ok = False
    database_status = "connected" if database_ok else "error"

    poller = app_state.get("poller")
    monitor_status = "running" if poller is not None and poller.last_poll_at is not None else (
        "starting" if poller is not None else "stopped"
    )

    if settings.fcm_dry_run:
        firebase_status = "dry-run"
    elif os.path.exists(settings.firebase_credentials_path):
        firebase_status = "configured"
    else:
        firebase_status = "not-configured"

    try:
        units_count = len(repository.list_units()) if database_ok else 0
    except Exception:
        units_count = 0
        database_status = "error"

    overall_status = "ok" if database_ok else "degraded"

    return HealthResponse(
        status=overall_status,
        database=database_status,
        monitor=monitor_status,
        firebase=firebase_status,
        source_mode=settings.source_mode,
        db_backend=settings.db_backend,
        last_poll_at=poller.last_poll_at if poller else None,
        units_count=units_count,
    )
