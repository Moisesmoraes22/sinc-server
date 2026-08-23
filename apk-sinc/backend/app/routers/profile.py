from fastapi import APIRouter, Depends

from app.database.base import Repository
from app.dependencies import get_repository
from app.models.schemas import ProfileOut, ProfileUpdateRequest

router = APIRouter(prefix="/api/profile", tags=["profile"])


@router.get("", response_model=ProfileOut)
def get_profile(repository: Repository = Depends(get_repository)) -> ProfileOut:
    profile = repository.get_or_create_profile()
    return ProfileOut.model_validate(profile)


@router.put("", response_model=ProfileOut)
def update_profile(
    payload: ProfileUpdateRequest, repository: Repository = Depends(get_repository)
) -> ProfileOut:
    profile = repository.update_profile(payload.display_name)
    return ProfileOut.model_validate(profile)
