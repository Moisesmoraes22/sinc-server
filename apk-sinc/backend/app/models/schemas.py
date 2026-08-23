from datetime import date, datetime

from pydantic import BaseModel


# ---------------------------------------------------------------------
# Perfil
# ---------------------------------------------------------------------
class ProfileOut(BaseModel):
    id: str | None
    display_name: str

    model_config = {"from_attributes": True}


class ProfileUpdateRequest(BaseModel):
    display_name: str


# ---------------------------------------------------------------------
# Habitos
# ---------------------------------------------------------------------
class HabitOut(BaseModel):
    id: str | None
    title: str
    category: str
    icon_key: str
    target_value: float | None
    target_unit: str | None
    color_tag: str
    today_value: float | None = None
    today_completed: bool = False


class HabitListResponse(BaseModel):
    status: str = "ok"
    habits: list[HabitOut]


class HabitCreateRequest(BaseModel):
    title: str
    category: str
    icon_key: str = "circle"
    target_value: float | None = None
    target_unit: str | None = None
    color_tag: str = "ACCENT"


class HabitLogRequest(BaseModel):
    value: float | None = None
    completed: bool = True


class HabitLogOut(BaseModel):
    id: int | None
    habit_id: str
    log_date: date
    value: float | None
    completed: bool
    logged_at: datetime | None

    model_config = {"from_attributes": True}


class HabitLogListResponse(BaseModel):
    status: str = "ok"
    logs: list[HabitLogOut]


# ---------------------------------------------------------------------
# Metricas de saude/bem-estar
# ---------------------------------------------------------------------
class MetricOut(BaseModel):
    id: int | None
    metric_type: str
    value: float
    recorded_at: datetime | None

    model_config = {"from_attributes": True}


class MetricListResponse(BaseModel):
    status: str = "ok"
    metrics: list[MetricOut]


class MetricCreateRequest(BaseModel):
    metric_type: str
    value: float


# ---------------------------------------------------------------------
# Home - resumo pessoal (agrega habitos + metricas + progresso semanal)
# ---------------------------------------------------------------------
class HomeMetricSnapshot(BaseModel):
    metric_type: str
    value: float | None
    recorded_at: datetime | None


class HomeSummaryResponse(BaseModel):
    status: str = "ok"
    greeting_name: str
    weekly_progress_pct: float
    weekly_progress_delta_pct: float
    habits_done_today: int
    habits_total_today: int
    next_action_habit_id: str | None
    next_action_message: str
    metrics: list[HomeMetricSnapshot]
    habits: list[HabitOut]


# ---------------------------------------------------------------------
# Dispositivos (push)
# ---------------------------------------------------------------------
class DeviceOut(BaseModel):
    id: str | None
    device_name: str | None
    active: bool
    created_at: datetime | None
    last_seen_at: datetime | None
    # fcm_token propositalmente omitido da resposta - e um segredo do
    # dispositivo, sem motivo para ser exposto de volta via API.

    model_config = {"from_attributes": True}


class DeviceListResponse(BaseModel):
    status: str = "ok"
    devices: list[DeviceOut]


class DeviceRegisterRequest(BaseModel):
    token: str
    platform: str = "android"


class HealthResponse(BaseModel):
    status: str
    database: str
    firebase: str
    db_backend: str
    habits_count: int
