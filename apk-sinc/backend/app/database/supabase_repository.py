"""Implementacao de Repository sobre Supabase (PostgreSQL), via supabase-py.

Esta e a implementacao usada em producao (db_backend=supabase). Os nomes de
tabela/coluna correspondem exatamente ao schema criado pelas migrations em
backend/supabase/migrations/.
"""

import logging
import time
from datetime import datetime
from typing import Any, Protocol

import httpx
from supabase import Client

from app.database.supabase_client import get_supabase_client
from app.models.domain import Device, MonitorSettings, SyncEvent, SyncUnit

logger = logging.getLogger("sinc.supabase_repository")

# Em producao (Windows) o cliente HTTP/2 usado pelo supabase-py falha de
# forma intermitente com "httpx.ReadError: [WinError 10035]" - um erro de
# socket nao-bloqueante que nao tem relacao com o dado sendo enviado, apenas
# com o momento da leitura da resposta. Sem retry, uma unica ocorrencia
# durante o ciclo de monitoramento derrubava o processamento daquela
# unidade (e, antes do isolamento por unidade, do ciclo inteiro),
# explicando unidades que ficavam paradas por horas/dias sem nenhum
# problema real na fonte de dados.
_RETRY_ATTEMPTS = 3
_RETRY_BASE_DELAY_SECONDS = 0.4


class _Executable(Protocol):
    def execute(self) -> Any: ...


def _execute_with_retry(query: _Executable) -> Any:
    last_error: Exception | None = None
    for attempt in range(_RETRY_ATTEMPTS):
        try:
            return query.execute()
        except httpx.TransportError as exc:
            last_error = exc
            if attempt < _RETRY_ATTEMPTS - 1:
                logger.warning(
                    "Erro de rede transitorio ao chamar o Supabase (tentativa %d/%d): %s",
                    attempt + 1,
                    _RETRY_ATTEMPTS,
                    exc,
                )
                time.sleep(_RETRY_BASE_DELAY_SECONDS * (attempt + 1))
    assert last_error is not None
    raise last_error


def _parse_dt(value: str | None) -> datetime | None:
    if not value:
        return None
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def _iso(value: datetime | None) -> str | None:
    return value.isoformat() if value else None


def _unit_from_row(row: dict) -> SyncUnit:
    return SyncUnit(
        id=row.get("id"),
        a7_code=row["a7_code"],
        business_unit=row["business_unit"],
        revisions_to_send=row.get("revisions_to_send"),
        last_send_at=_parse_dt(row.get("last_send_at")),
        last_receive_at=_parse_dt(row.get("last_receive_at")),
        send_elapsed_minutes=row.get("send_elapsed_minutes"),
        receive_elapsed_minutes=row.get("receive_elapsed_minutes"),
        send_status=row.get("send_status", "NORMAL"),
        receive_status=row.get("receive_status", "NORMAL"),
        overall_status=row.get("overall_status", "NORMAL"),
        consecutive_failures=row.get("consecutive_failures", 0),
        last_checked_at=_parse_dt(row.get("last_checked_at")),
        created_at=_parse_dt(row.get("created_at")),
        updated_at=_parse_dt(row.get("updated_at")),
    )


def _event_from_row(row: dict) -> SyncEvent:
    return SyncEvent(
        id=row.get("id"),
        unit_id=row.get("unit_id"),
        a7_code=row["a7_code"],
        business_unit=row["business_unit"],
        event_type=row["event_type"],
        direction=row["direction"],
        previous_status=row.get("previous_status"),
        new_status=row["new_status"],
        started_at=_parse_dt(row.get("started_at")),
        resolved_at=_parse_dt(row.get("resolved_at")),
        duration_seconds=row.get("duration_seconds"),
        message=row.get("message"),
        created_at=_parse_dt(row.get("created_at")),
    )


def _device_from_row(row: dict) -> Device:
    return Device(
        id=row.get("id"),
        device_name=row.get("device_name"),
        fcm_token=row["fcm_token"],
        active=row.get("active", True),
        created_at=_parse_dt(row.get("created_at")),
        updated_at=_parse_dt(row.get("updated_at")),
        last_seen_at=_parse_dt(row.get("last_seen_at")),
    )


class SupabaseRepository:
    def __init__(self, client: Client | None = None):
        self._client = client or get_supabase_client()

    def get_unit(self, a7_code: str) -> SyncUnit | None:
        result = _execute_with_retry(self._client.table("sync_units").select("*").eq("a7_code", a7_code).limit(1))
        return _unit_from_row(result.data[0]) if result.data else None

    def list_units(self) -> list[SyncUnit]:
        result = _execute_with_retry(self._client.table("sync_units").select("*").order("business_unit"))
        return [_unit_from_row(r) for r in result.data]

    def upsert_unit(self, unit: SyncUnit) -> SyncUnit:
        payload = {
            "a7_code": unit.a7_code,
            "business_unit": unit.business_unit,
            "revisions_to_send": unit.revisions_to_send,
            "last_send_at": _iso(unit.last_send_at),
            "last_receive_at": _iso(unit.last_receive_at),
            "send_elapsed_minutes": unit.send_elapsed_minutes,
            "receive_elapsed_minutes": unit.receive_elapsed_minutes,
            "send_status": unit.send_status,
            "receive_status": unit.receive_status,
            "overall_status": unit.overall_status,
            "consecutive_failures": unit.consecutive_failures,
            "last_checked_at": _iso(unit.last_checked_at),
        }
        result = _execute_with_retry(self._client.table("sync_units").upsert(payload, on_conflict="a7_code"))
        return _unit_from_row(result.data[0])

    def add_event(self, event: SyncEvent) -> SyncEvent:
        payload = {
            "unit_id": event.unit_id,
            "a7_code": event.a7_code,
            "business_unit": event.business_unit,
            "event_type": event.event_type,
            "direction": event.direction,
            "previous_status": event.previous_status,
            "new_status": event.new_status,
            "started_at": _iso(event.started_at),
            "resolved_at": _iso(event.resolved_at),
            "duration_seconds": event.duration_seconds,
            "message": event.message,
        }
        result = _execute_with_retry(self._client.table("sync_events").insert(payload))
        return _event_from_row(result.data[0])

    def list_events(
        self, unit_a7_code: str | None = None, status: str | None = None, limit: int = 200
    ) -> list[SyncEvent]:
        query = self._client.table("sync_events").select("*").order("created_at", desc=True).limit(limit)
        if unit_a7_code:
            query = query.eq("a7_code", unit_a7_code)
        if status:
            query = query.eq("new_status", status)
        result = _execute_with_retry(query)
        return [_event_from_row(r) for r in result.data]

    def get_last_event_for_unit(self, a7_code: str) -> SyncEvent | None:
        result = _execute_with_retry(
            self._client.table("sync_events")
            .select("*")
            .eq("a7_code", a7_code)
            .order("created_at", desc=True)
            .limit(1)
        )
        return _event_from_row(result.data[0]) if result.data else None

    def register_device(self, fcm_token: str, device_name: str | None = None) -> Device:
        now = datetime.utcnow().isoformat()
        payload = {"fcm_token": fcm_token, "device_name": device_name, "active": True, "last_seen_at": now}
        result = _execute_with_retry(self._client.table("devices").upsert(payload, on_conflict="fcm_token"))
        return _device_from_row(result.data[0])

    def list_devices(self) -> list[Device]:
        result = _execute_with_retry(self._client.table("devices").select("*").order("created_at"))
        return [_device_from_row(r) for r in result.data]

    def list_active_device_tokens(self) -> list[str]:
        result = _execute_with_retry(self._client.table("devices").select("fcm_token").eq("active", True))
        return [r["fcm_token"] for r in result.data]

    def get_settings(self) -> MonitorSettings:
        result = _execute_with_retry(self._client.table("monitor_settings").select("*").eq("id", 1).limit(1))
        if not result.data:
            return MonitorSettings()
        row = result.data[0]
        return MonitorSettings(
            warning_threshold_minutes=row["warning_threshold_minutes"],
            critical_threshold_minutes=row["critical_threshold_minutes"],
            check_interval_seconds=row["check_interval_seconds"],
        )

    def update_settings(self, settings: MonitorSettings) -> MonitorSettings:
        payload = {
            "id": 1,
            "warning_threshold_minutes": settings.warning_threshold_minutes,
            "critical_threshold_minutes": settings.critical_threshold_minutes,
            "check_interval_seconds": settings.check_interval_seconds,
        }
        _execute_with_retry(self._client.table("monitor_settings").upsert(payload))
        return settings

    def health_check(self) -> bool:
        try:
            _execute_with_retry(self._client.table("monitor_settings").select("id").limit(1))
            return True
        except Exception:
            return False
