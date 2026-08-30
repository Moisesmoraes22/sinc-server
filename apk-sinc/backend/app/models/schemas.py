from datetime import datetime

from pydantic import BaseModel


# ---------------------------------------------------------------------
# Dominio novo: unidades de sincronizacao (sync_units / sync_events)
# ---------------------------------------------------------------------
class SyncUnitOut(BaseModel):
    id: str | None
    a7_code: str
    business_unit: str
    revisions_to_send: int | None
    last_send_at: datetime | None
    last_receive_at: datetime | None
    send_elapsed_minutes: float | None
    receive_elapsed_minutes: float | None
    send_status: str
    receive_status: str
    overall_status: str
    consecutive_failures: int
    last_checked_at: datetime | None

    model_config = {"from_attributes": True}


class SyncUnitListResponse(BaseModel):
    status: str = "ok"
    units: list[SyncUnitOut]


class SyncEventOut(BaseModel):
    id: int | None
    unit_id: str | None
    a7_code: str
    business_unit: str
    event_type: str
    direction: str
    previous_status: str | None
    new_status: str
    started_at: datetime | None
    resolved_at: datetime | None
    duration_seconds: int | None
    message: str | None
    created_at: datetime | None

    model_config = {"from_attributes": True}


class SyncEventListResponse(BaseModel):
    status: str = "ok"
    events: list[SyncEventOut]


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


class MonitorSettingsOut(BaseModel):
    warning_threshold_minutes: int
    critical_threshold_minutes: int
    check_interval_seconds: int

    model_config = {"from_attributes": True}


# ---------------------------------------------------------------------
# Compatibilidade com o app Android existente (dominio "servers")
# GET /api/servers e GET /api/events continuam com este formato - o
# aplicativo Android nao precisa de nenhuma alteracao.
# ---------------------------------------------------------------------
class ServerOut(BaseModel):
    id: str
    name: str
    status: str
    response_time_ms: int | None
    last_check: datetime | None
    last_down_at: datetime | None
    last_up_at: datetime | None
    down_count: int
    # Campos adicionais (aditivos, nao quebram o app antigo): detalham as
    # duas direcoes de sincronizacao separadamente, para telas que queiram
    # mostrar envio e recebimento como cartoes distintos.
    send_status: str | None = None
    receive_status: str | None = None
    send_elapsed_minutes: float | None = None
    receive_elapsed_minutes: float | None = None
    last_send_at: datetime | None = None
    last_receive_at: datetime | None = None
    warning_threshold_minutes: int | None = None
    critical_threshold_minutes: int | None = None
    revisions_to_send: int | None = None


class ServerListResponse(BaseModel):
    status: str = "ok"
    servers: list[ServerOut]


class EventOut(BaseModel):
    id: int
    server_id: str
    server_name: str
    from_status: str
    to_status: str
    occurred_at: datetime
    reason: str | None
    response_time_ms: int | None
    duration_seconds: int | None = None


class EventListResponse(BaseModel):
    status: str = "ok"
    events: list[EventOut]


class HealthResponse(BaseModel):
    status: str
    database: str
    monitor: str
    firebase: str
    source_mode: str
    db_backend: str
    last_poll_at: datetime | None
    last_successful_fetch_at: datetime | None
    consecutive_fetch_failures: int
    units_count: int


class DeviceRegisterRequest(BaseModel):
    token: str
    platform: str = "android"


class UptimeOut(BaseModel):
    period_days: int
    uptime_percent: float
    downtime_seconds: int


class MockScenarioRequest(BaseModel):
    """Permite forcar um cenario de teste no MockSourceClient em runtime."""

    a7_code: str
    sequence: list[str]  # ex: ["NORMAL", "ATENCAO", "CRITICO", "CRITICO", "NORMAL"]
