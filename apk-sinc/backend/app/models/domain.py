"""Modelos de dominio, independentes de qualquer tecnologia de banco.

Esta camada existe para que a logica de negocio (maquina de estados,
monitor_service) nunca dependa diretamente de SQLAlchemy ou do cliente
Supabase - ela conversa apenas com estes dataclasses e com a interface
`Repository` (app/database/base.py). Isso permite trocar o backend de dados
(SQLite <-> Supabase) sem tocar na regra de negocio.
"""

from dataclasses import dataclass, field
from datetime import datetime

NORMAL = "NORMAL"
ATENCAO = "ATENCAO"
CRITICO = "CRITICO"

VALID_STATUSES = (NORMAL, ATENCAO, CRITICO)

EVENT_STATUS_CHANGE = "STATUS_CHANGE"
EVENT_PROBLEM_STARTED = "PROBLEM_STARTED"
EVENT_PROBLEM_RESOLVED = "PROBLEM_RESOLVED"

DIRECTION_SEND = "SEND"
DIRECTION_RECEIVE = "RECEIVE"
DIRECTION_BOTH = "BOTH"


@dataclass
class SyncUnit:
    a7_code: str
    business_unit: str
    id: str | None = None
    revisions_to_send: int | None = None
    last_send_at: datetime | None = None
    last_receive_at: datetime | None = None
    send_elapsed_minutes: float | None = None
    receive_elapsed_minutes: float | None = None
    send_status: str = NORMAL
    receive_status: str = NORMAL
    overall_status: str = NORMAL
    consecutive_failures: int = 0
    last_checked_at: datetime | None = None
    created_at: datetime | None = None
    updated_at: datetime | None = None


@dataclass
class SyncEvent:
    a7_code: str
    business_unit: str
    event_type: str
    direction: str
    new_status: str
    id: int | None = None
    unit_id: str | None = None
    previous_status: str | None = None
    started_at: datetime | None = None
    resolved_at: datetime | None = None
    duration_seconds: int | None = None
    message: str | None = None
    created_at: datetime | None = None


@dataclass
class Device:
    fcm_token: str
    id: str | None = None
    device_name: str | None = None
    active: bool = True
    created_at: datetime | None = None
    updated_at: datetime | None = None
    last_seen_at: datetime | None = None


@dataclass
class MonitorSettings:
    warning_threshold_minutes: int = 5
    critical_threshold_minutes: int = 15
    check_interval_seconds: int = 60


@dataclass
class SyncUnitReading:
    """Leitura normalizada vinda da fonte (real ou mock), antes de aplicar a maquina de estados."""

    a7_code: str
    business_unit: str
    revisions_to_send: int | None = None
    last_send_at: datetime | None = None
    last_receive_at: datetime | None = None
    checked_at: datetime | None = None
    raw: dict = field(default_factory=dict)
