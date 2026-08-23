from fastapi import APIRouter, Depends

from app.database.base import Repository
from app.dependencies import get_repository
from app.models.schemas import HomeSummaryResponse
from app.services.habit_service import compute_home_summary

router = APIRouter(prefix="/api/home", tags=["home"])


@router.get("/summary", response_model=HomeSummaryResponse)
def home_summary(repository: Repository = Depends(get_repository)) -> HomeSummaryResponse:
    profile = repository.get_or_create_profile()
    return compute_home_summary(repository, profile)
