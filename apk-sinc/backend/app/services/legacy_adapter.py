"""Adapta o dominio novo (sync_units / sync_events, status NORMAL/ATENCAO/
CRITICO) para o formato de API que o app Android ja consome desde a
primeira versao do projeto (servers / events, status ONLINE/ATENCAO/
OFFLINE). Isso permite migrar o backend para Supabase e para o dominio de
sincronizacao sem exigir nenhuma mudanca no aplicativo Android existente.
"""

from app.database.base import Repository
from app.models.domain import ATENCAO, CRITICO, EVENT_PROBLEM_STARTED, NORMAL, SyncEvent, SyncUnit
from app.models.schemas import EventOut, ServerOut

_STATUS_TO_LEGACY = {
    NORMAL: "ONLINE",
    ATENCAO: "ATENCAO",
    CRITICO: "OFFLINE",
}


def status_to_legacy(status: str) -> str:
    return _STATUS_TO_LEGACY.get(status, status)


def unit_to_server_out(
    unit: SyncUnit,
    repository: Repository | None = None,
    warning_threshold_minutes: int | None = None,
    critical_threshold_minutes: int | None = None,
) -> ServerOut:
    """Converte uma SyncUnit para o formato legado ServerOut.

    Se `repository` for informado, `last_down_at`/`last_up_at`/`down_count`
    sao calculados com precisao a partir do historico de eventos (usado no
    endpoint de detalhe, `GET /api/servers/{id}`). Sem repository, usa uma
    aproximacao barata baseada apenas no estado atual da unidade (usado na
    listagem, `GET /api/servers`, para nao gerar N+1 consultas).
    """
    last_down_at = None
    last_up_at = None
    down_count = 0

    if repository is not None:
        events = repository.list_events(unit_a7_code=unit.a7_code, limit=500)
        for event in events:
            if event.new_status == CRITICO and last_down_at is None:
                last_down_at = event.started_at or event.created_at
            if event.new_status == NORMAL and last_up_at is None:
                last_up_at = event.resolved_at or event.created_at
        down_count = sum(1 for event in events if event.event_type == EVENT_PROBLEM_STARTED)
    else:
        if unit.overall_status == CRITICO:
            last_down_at = unit.last_checked_at
        elif unit.overall_status == NORMAL and unit.consecutive_failures == 0:
            last_up_at = unit.last_checked_at

    return ServerOut(
        id=unit.a7_code,
        name=unit.business_unit,
        status=status_to_legacy(unit.overall_status),
        response_time_ms=None,
        last_check=unit.last_checked_at,
        last_down_at=last_down_at,
        last_up_at=last_up_at,
        down_count=down_count,
        send_status=status_to_legacy(unit.send_status),
        receive_status=status_to_legacy(unit.receive_status),
        send_elapsed_minutes=unit.send_elapsed_minutes,
        receive_elapsed_minutes=unit.receive_elapsed_minutes,
        last_send_at=unit.last_send_at,
        last_receive_at=unit.last_receive_at,
        warning_threshold_minutes=warning_threshold_minutes,
        critical_threshold_minutes=critical_threshold_minutes,
        revisions_to_send=unit.revisions_to_send,
    )


def event_to_event_out(event: SyncEvent) -> EventOut:
    return EventOut(
        id=event.id or 0,
        server_id=event.a7_code,
        server_name=event.business_unit,
        from_status=status_to_legacy(event.previous_status or NORMAL),
        to_status=status_to_legacy(event.new_status),
        occurred_at=event.created_at or event.started_at or event.resolved_at,
        reason=event.message,
        response_time_ms=None,
        duration_seconds=event.duration_seconds,
    )
