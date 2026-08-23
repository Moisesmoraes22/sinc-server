import logging
from datetime import datetime, timezone

from app.database.base import Repository
from app.models.domain import (
    DIRECTION_BOTH,
    DIRECTION_RECEIVE,
    DIRECTION_SEND,
    EVENT_PROBLEM_RESOLVED,
    EVENT_PROBLEM_STARTED,
    EVENT_STATUS_CHANGE,
    NORMAL,
    MonitorSettings,
    SyncEvent,
    SyncUnit,
    SyncUnitReading,
)
from app.monitor.source_client import SourceClient
from app.notifications.fcm_client import FcmClient
from app.services.state_machine import evaluate

logger = logging.getLogger("sinc.monitor_service")


def _elapsed_minutes(reference: datetime | None, now: datetime) -> float | None:
    if reference is None:
        return None
    if reference.tzinfo is None:
        reference = reference.replace(tzinfo=timezone.utc)
    return round((now - reference).total_seconds() / 60, 2)


def _direction_for(send_status: str, receive_status: str) -> str:
    send_bad = send_status != NORMAL
    receive_bad = receive_status != NORMAL
    if send_bad and receive_bad:
        return DIRECTION_BOTH
    if send_bad:
        return DIRECTION_SEND
    if receive_bad:
        return DIRECTION_RECEIVE
    return DIRECTION_BOTH


def _event_type_for(previous_status: str, new_status: str) -> str:
    if previous_status == NORMAL and new_status != NORMAL:
        return EVENT_PROBLEM_STARTED
    if previous_status != NORMAL and new_status == NORMAL:
        return EVENT_PROBLEM_RESOLVED
    return EVENT_STATUS_CHANGE


def _build_message(a7_code: str, previous_status: str, new_status: str) -> str:
    return f"Unidade {a7_code}: {previous_status} -> {new_status}"


class MonitorService:
    def __init__(self, source_client: SourceClient, repository: Repository, fcm_client: FcmClient | None = None):
        self.source_client = source_client
        self.repository = repository
        self.fcm_client = fcm_client or FcmClient()

    async def run_check_cycle(self) -> list[SyncUnitReading]:
        """Busca a fonte, aplica a maquina de estados por unidade, persiste e notifica.

        Erros de rede ao buscar a fonte inteira sao logados e absorvidos,
        sem interromper o processo (o proximo ciclo tenta de novo).
        """
        try:
            readings = await self.source_client.fetch_units()
        except Exception:
            logger.exception("Falha ao consultar a fonte de monitoramento")
            return []

        settings = self.repository.get_settings()

        for reading in readings:
            self._process_reading(reading, settings)

        return readings

    def _process_reading(self, reading: SyncUnitReading, settings: MonitorSettings) -> None:
        now = reading.checked_at or datetime.now(timezone.utc)
        existing = self.repository.get_unit(reading.a7_code)
        previous_overall = existing.overall_status if existing else NORMAL

        last_send_at = reading.last_send_at or (existing.last_send_at if existing else None)
        last_receive_at = reading.last_receive_at or (existing.last_receive_at if existing else None)
        send_elapsed = _elapsed_minutes(last_send_at, now)
        receive_elapsed = _elapsed_minutes(last_receive_at, now)

        result = evaluate(
            previous_overall_status=previous_overall,
            send_elapsed_minutes=send_elapsed,
            receive_elapsed_minutes=receive_elapsed,
            warning_threshold_minutes=settings.warning_threshold_minutes,
            critical_threshold_minutes=settings.critical_threshold_minutes,
        )

        consecutive_failures = existing.consecutive_failures if existing else 0
        consecutive_failures = consecutive_failures + 1 if result.overall_status != NORMAL else 0

        unit = SyncUnit(
            id=existing.id if existing else None,
            a7_code=reading.a7_code,
            business_unit=reading.business_unit,
            revisions_to_send=reading.revisions_to_send,
            last_send_at=last_send_at,
            last_receive_at=last_receive_at,
            send_elapsed_minutes=send_elapsed,
            receive_elapsed_minutes=receive_elapsed,
            send_status=result.send_status,
            receive_status=result.receive_status,
            overall_status=result.overall_status,
            consecutive_failures=consecutive_failures,
            last_checked_at=now,
        )
        saved = self.repository.upsert_unit(unit)

        if not result.should_notify:
            return

        event_type = _event_type_for(previous_overall, result.overall_status)
        direction = _direction_for(result.send_status, result.receive_status)
        started_at, duration_seconds = self._resolve_duration(reading.a7_code, event_type, now)

        event = SyncEvent(
            unit_id=saved.id,
            a7_code=reading.a7_code,
            business_unit=reading.business_unit,
            event_type=event_type,
            direction=direction,
            previous_status=previous_overall,
            new_status=result.overall_status,
            started_at=started_at if event_type == EVENT_PROBLEM_STARTED else None,
            resolved_at=now if event_type == EVENT_PROBLEM_RESOLVED else None,
            duration_seconds=duration_seconds,
            message=_build_message(reading.a7_code, previous_overall, result.overall_status),
        )
        self.repository.add_event(event)
        logger.info(
            "Unidade %s (%s): %s -> %s",
            reading.business_unit,
            reading.a7_code,
            previous_overall,
            result.overall_status,
        )

        self._notify(saved, previous_overall, result.overall_status)

    def _resolve_duration(
        self, a7_code: str, event_type: str, now: datetime
    ) -> tuple[datetime | None, int | None]:
        """Ao resolver um problema, calcula ha quanto tempo ele estava aberto,
        olhando o ultimo evento registrado para a unidade (deve ser o
        PROBLEM_STARTED correspondente, ja que nao ha renotificacao entre
        eles - ver anti-spam)."""
        if event_type != EVENT_PROBLEM_RESOLVED:
            return None, None

        last_event = self.repository.get_last_event_for_unit(a7_code)
        if last_event is None:
            return None, None
        started = last_event.started_at or last_event.created_at
        if started is None:
            return None, None
        if started.tzinfo is None and now.tzinfo is not None:
            started = started.replace(tzinfo=now.tzinfo)
        duration = int((now - started).total_seconds())
        return started, max(duration, 0)

    def _notify(self, unit: SyncUnit, previous_status: str, new_status: str) -> None:
        tokens = self.repository.list_active_device_tokens()
        self.fcm_client.send_status_change(unit.a7_code, unit.business_unit, new_status, tokens)
