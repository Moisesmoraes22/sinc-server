from datetime import datetime

from pydantic import BaseModel


class ServerReading(BaseModel):
    """Leitura normalizada vinda da fonte (real ou mock), antes de aplicar a maquina de estados."""

    external_id: str
    name: str
    raw_status: str
    is_up: bool
    response_time_ms: int | None = None
    checked_at: datetime


class ServerOut(BaseModel):
    id: str
    name: str
    status: str
    response_time_ms: int | None
    last_check: datetime | None
    last_down_at: datetime | None
    last_up_at: datetime | None
    down_count: int

    model_config = {"from_attributes": True}


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

    model_config = {"from_attributes": True}


class EventListResponse(BaseModel):
    status: str = "ok"
    events: list[EventOut]


class HealthResponse(BaseModel):
    status: str = "ok"
    source_mode: str
    last_poll_at: datetime | None
    servers_count: int


class DeviceRegisterRequest(BaseModel):
    token: str
    platform: str = "android"


class MockScenarioRequest(BaseModel):
    """Permite forcar um cenario de teste no MockSourceClient em runtime."""

    server_id: str
    sequence: list[str]  # ex: ["up", "down", "down", "down", "up"]
