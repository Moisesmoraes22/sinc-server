"""Modelos de dominio, independentes de qualquer tecnologia de banco.

Esta camada existe para que a logica de negocio (services/habit_service.py)
nunca dependa diretamente de SQLAlchemy ou do cliente Supabase - ela
conversa apenas com estes dataclasses e com a interface `Repository`
(app/database/base.py). Isso permite trocar o backend de dados (SQLite <->
Supabase) sem tocar na regra de negocio.

Dominio: OMG SINC e um app pessoal de saude/beleza/bem-estar. O usuario
acompanha habitos (agua, sono, skincare, exercicio, humor...) e metricas de
saude ao longo do tempo. Nao ha "monitoramento" de fontes externas - todo
dado nasce de uma acao do proprio usuario no app.
"""

from dataclasses import dataclass
from datetime import date, datetime

# Categorias de habito - determinam o icone/agrupamento nas telas de Saude,
# Bem-estar e Rotina.
CATEGORY_AGUA = "AGUA"
CATEGORY_SONO = "SONO"
CATEGORY_EXERCICIO = "EXERCICIO"
CATEGORY_SKINCARE = "SKINCARE"
CATEGORY_HUMOR = "HUMOR"
CATEGORY_OUTRO = "OUTRO"

VALID_CATEGORIES = (
    CATEGORY_AGUA,
    CATEGORY_SONO,
    CATEGORY_EXERCICIO,
    CATEGORY_SKINCARE,
    CATEGORY_HUMOR,
    CATEGORY_OUTRO,
)

# Cor de destaque do habito na UI - usa apenas os tokens funcionais oficiais
# (nunca uma cor decorativa nova).
COLOR_ACCENT = "ACCENT"
COLOR_SUCCESS = "SUCCESS"
COLOR_WARNING = "WARNING"

VALID_COLOR_TAGS = (COLOR_ACCENT, COLOR_SUCCESS, COLOR_WARNING)

# Tipos de metrica registrados ao longo do tempo (series historicas simples).
METRIC_SLEEP_HOURS = "SLEEP_HOURS"
METRIC_WATER_ML = "WATER_ML"
METRIC_MOOD_SCORE = "MOOD_SCORE"
METRIC_STEPS = "STEPS"
METRIC_WEIGHT_KG = "WEIGHT_KG"

VALID_METRIC_TYPES = (
    METRIC_SLEEP_HOURS,
    METRIC_WATER_ML,
    METRIC_MOOD_SCORE,
    METRIC_STEPS,
    METRIC_WEIGHT_KG,
)


@dataclass
class Profile:
    display_name: str
    id: str | None = None
    created_at: datetime | None = None
    updated_at: datetime | None = None


@dataclass
class Habit:
    profile_id: str
    title: str
    category: str
    id: str | None = None
    icon_key: str = "circle"
    target_value: float | None = None
    target_unit: str | None = None
    color_tag: str = COLOR_ACCENT
    active: bool = True
    sort_order: int = 0
    created_at: datetime | None = None
    updated_at: datetime | None = None


@dataclass
class HabitLog:
    habit_id: str
    log_date: date
    id: int | None = None
    value: float | None = None
    completed: bool = False
    logged_at: datetime | None = None


@dataclass
class Metric:
    profile_id: str
    metric_type: str
    value: float
    id: int | None = None
    recorded_at: datetime | None = None


@dataclass
class Device:
    fcm_token: str
    id: str | None = None
    device_name: str | None = None
    active: bool = True
    created_at: datetime | None = None
    updated_at: datetime | None = None
    last_seen_at: datetime | None = None
