from datetime import date, timedelta

from fastapi import APIRouter, Depends, HTTPException

from app.database.base import Repository
from app.dependencies import get_repository
from app.models.domain import VALID_METRIC_TYPES, Metric
from app.models.schemas import MetricCreateRequest, MetricListResponse, MetricOut

router = APIRouter(prefix="/api/metrics", tags=["metrics"])


@router.get("", response_model=MetricListResponse)
def list_metrics(
    metric_type: str | None = None, days: int = 30, repository: Repository = Depends(get_repository)
) -> MetricListResponse:
    profile = repository.get_or_create_profile()
    since = date.today() - timedelta(days=days)
    metrics = repository.list_metrics(profile.id, metric_type=metric_type, since=since)
    return MetricListResponse(metrics=[MetricOut.model_validate(m) for m in metrics])


@router.post("", response_model=MetricOut)
def create_metric(payload: MetricCreateRequest, repository: Repository = Depends(get_repository)) -> MetricOut:
    if payload.metric_type not in VALID_METRIC_TYPES:
        raise HTTPException(status_code=422, detail=f"metric_type invalido: {payload.metric_type}")

    profile = repository.get_or_create_profile()
    metric = repository.add_metric(Metric(profile_id=profile.id, metric_type=payload.metric_type, value=payload.value))
    return MetricOut.model_validate(metric)
