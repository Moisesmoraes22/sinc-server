import os

from fastapi import APIRouter, Depends

from app.config import get_settings
from app.database.base import Repository
from app.dependencies import get_repository
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

    if settings.fcm_dry_run:
        firebase_status = "dry-run"
    elif os.path.exists(settings.firebase_credentials_path):
        firebase_status = "configured"
    else:
        firebase_status = "not-configured"

    try:
        profile = repository.get_or_create_profile() if database_ok else None
        habits_count = len(repository.list_habits(profile.id)) if profile else 0
    except Exception:
        habits_count = 0
        database_status = "error"

    overall_status = "ok" if database_ok else "degraded"

    return HealthResponse(
        status=overall_status,
        database=database_status,
        firebase=firebase_status,
        db_backend=settings.db_backend,
        habits_count=habits_count,
    )
