from datetime import date, timedelta

from fastapi import APIRouter, Depends, HTTPException

from app.database.base import Repository
from app.dependencies import get_repository
from app.models.domain import VALID_CATEGORIES, VALID_COLOR_TAGS, Habit
from app.models.schemas import (
    HabitCreateRequest,
    HabitLogListResponse,
    HabitLogOut,
    HabitLogRequest,
    HabitListResponse,
    HabitOut,
)
from app.services.habit_service import habit_to_out

router = APIRouter(prefix="/api/habits", tags=["habits"])


@router.get("", response_model=HabitListResponse)
def list_habits(repository: Repository = Depends(get_repository)) -> HabitListResponse:
    profile = repository.get_or_create_profile()
    today = date.today()
    habits = repository.list_habits(profile.id)
    outs = [habit_to_out(h, repository.get_habit_log(h.id, today)) for h in habits]
    return HabitListResponse(habits=outs)


@router.post("", response_model=HabitOut)
def create_habit(payload: HabitCreateRequest, repository: Repository = Depends(get_repository)) -> HabitOut:
    if payload.category not in VALID_CATEGORIES:
        raise HTTPException(status_code=422, detail=f"category invalida: {payload.category}")
    if payload.color_tag not in VALID_COLOR_TAGS:
        raise HTTPException(status_code=422, detail=f"color_tag invalido: {payload.color_tag}")

    profile = repository.get_or_create_profile()
    habit = repository.upsert_habit(
        Habit(
            profile_id=profile.id,
            title=payload.title,
            category=payload.category,
            icon_key=payload.icon_key,
            target_value=payload.target_value,
            target_unit=payload.target_unit,
            color_tag=payload.color_tag,
        )
    )
    return habit_to_out(habit, today_log=None)


@router.post("/{habit_id}/log", response_model=HabitOut)
def log_habit(
    habit_id: str, payload: HabitLogRequest, repository: Repository = Depends(get_repository)
) -> HabitOut:
    habit = repository.get_habit(habit_id)
    if habit is None:
        raise HTTPException(status_code=404, detail="Habito nao encontrado")

    repository.log_habit(habit_id=habit_id, log_date=date.today(), value=payload.value, completed=payload.completed)
    today_log = repository.get_habit_log(habit_id, date.today())
    return habit_to_out(habit, today_log)


@router.get("/{habit_id}/logs", response_model=HabitLogListResponse)
def list_habit_logs(
    habit_id: str, days: int = 30, repository: Repository = Depends(get_repository)
) -> HabitLogListResponse:
    since = date.today() - timedelta(days=days)
    logs = repository.list_habit_logs(habit_id=habit_id, since=since)
    return HabitLogListResponse(logs=[HabitLogOut.model_validate(log) for log in logs])
