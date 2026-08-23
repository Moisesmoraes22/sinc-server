"""Interface comum entre o repositorio Supabase (producao) e o SQLite (testes/dev local).

A logica de negocio (services/monitor_service.py, routers/*) depende
apenas desta interface, nunca de SQLAlchemy ou do cliente Supabase
diretamente - troca de banco = trocar a implementacao, sem tocar em regra
de negocio.
"""

from typing import Protocol

from app.models.domain import Device, MonitorSettings, SyncEvent, SyncUnit


class Repository(Protocol):
    def get_unit(self, a7_code: str) -> SyncUnit | None: ...

    def list_units(self) -> list[SyncUnit]: ...

    def upsert_unit(self, unit: SyncUnit) -> SyncUnit: ...

    def add_event(self, event: SyncEvent) -> SyncEvent: ...

    def list_events(
        self,
        unit_a7_code: str | None = None,
        status: str | None = None,
        limit: int = 200,
    ) -> list[SyncEvent]: ...

    def get_last_event_for_unit(self, a7_code: str) -> SyncEvent | None: ...

    def register_device(self, fcm_token: str, device_name: str | None = None) -> Device: ...

    def list_devices(self) -> list[Device]: ...

    def list_active_device_tokens(self) -> list[str]: ...

    def get_settings(self) -> MonitorSettings: ...

    def health_check(self) -> bool:
        """Retorna True se a conexao com o banco esta funcionando."""
        ...
