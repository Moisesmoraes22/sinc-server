"""Interface comum entre o repositorio Supabase (producao) e o SQLite (testes/dev local).

A logica de negocio (services/habit_service.py, routers/*) depende apenas
desta interface, nunca de SQLAlchemy ou do cliente Supabase diretamente -
troca de banco = trocar a implementacao, sem tocar em regra de negocio.
"""

from datetime import date
from typing import Protocol

from app.models.domain import Device, Habit, HabitLog, Metric, Profile


class Repository(Protocol):
    def get_or_create_profile(self, display_name: str = "Voce") -> Profile: ...

    def update_profile(self, display_name: str) -> Profile: ...

    def list_habits(self, profile_id: str, active_only: bool = True) -> list[Habit]: ...

    def get_habit(self, habit_id: str) -> Habit | None: ...

    def upsert_habit(self, habit: Habit) -> Habit: ...

    def log_habit(
        self, habit_id: str, log_date: date, value: float | None, completed: bool
    ) -> HabitLog: ...

    def get_habit_log(self, habit_id: str, log_date: date) -> HabitLog | None: ...

    def list_habit_logs(
        self, habit_id: str | None = None, since: date | None = None, limit: int = 200
    ) -> list[HabitLog]: ...

    def add_metric(self, metric: Metric) -> Metric: ...

    def list_metrics(
        self, profile_id: str, metric_type: str | None = None, since: date | None = None, limit: int = 100
    ) -> list[Metric]: ...

    def latest_metric(self, profile_id: str, metric_type: str) -> Metric | None: ...

    def register_device(self, fcm_token: str, device_name: str | None = None) -> Device: ...

    def list_devices(self) -> list[Device]: ...

    def list_active_device_tokens(self) -> list[str]: ...

    def health_check(self) -> bool:
        """Retorna True se a conexao com o banco esta funcionando."""
        ...
