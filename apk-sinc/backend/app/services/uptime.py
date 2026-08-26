"""Calculo de disponibilidade (uptime) de uma unidade num periodo.

Reaproveita o historico ja gravado em sync_events (nenhum dado novo precisa
ser coletado da fonte): cada problema resolvido carrega `duration_seconds`
(quanto tempo ficou fora do NORMAL) e `resolved_at` (quando voltou). Somamos
essas duracoes, recortadas para caber dentro da janela pedida, e - se a
unidade estiver com problema aberto agora - contamos o tempo decorrido desde
o inicio desse problema ate agora tambem.
"""

from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

from app.models.domain import EVENT_PROBLEM_RESOLVED, EVENT_PROBLEM_STARTED, NORMAL, SyncEvent, SyncUnit

# Historico suficiente para cobrir a maior janela hoje suportada (30 dias)
# mesmo em unidades instaveis, sem paginar.
_EVENTS_LOOKBACK_LIMIT = 1000


@dataclass
class UptimeResult:
    period_days: int
    uptime_percent: float
    downtime_seconds: int


def _as_utc(dt: datetime) -> datetime:
    return dt if dt.tzinfo is not None else dt.replace(tzinfo=timezone.utc)


def _overlap_seconds(start: datetime, end: datetime, window_start: datetime, window_end: datetime) -> float:
    overlap_start = max(start, window_start)
    overlap_end = min(end, window_end)
    return max((overlap_end - overlap_start).total_seconds(), 0)


def compute_uptime(unit: SyncUnit, events: list[SyncEvent], days: int, now: datetime | None = None) -> UptimeResult:
    now = _as_utc(now or datetime.now(timezone.utc))
    window_start = now - timedelta(days=days)
    period_seconds = (now - window_start).total_seconds()

    downtime_seconds = 0.0
    for event in events:
        if event.event_type != EVENT_PROBLEM_RESOLVED or event.duration_seconds is None or event.resolved_at is None:
            continue
        resolved_at = _as_utc(event.resolved_at)
        started_at = resolved_at - timedelta(seconds=event.duration_seconds)
        downtime_seconds += _overlap_seconds(started_at, resolved_at, window_start, now)

    # Problema em aberto agora: soma o tempo desde que comecou (ou desde o
    # inicio da janela, o que for mais recente) ate agora. Encontrado
    # varrendo do evento mais novo para o mais antigo ate achar o
    # PROBLEM_STARTED correspondente - um PROBLEM_RESOLVED encontrado antes
    # dele significa que nao ha problema aberto.
    if unit.overall_status != NORMAL:
        for event in events:
            if event.event_type == EVENT_PROBLEM_RESOLVED:
                break
            if event.event_type == EVENT_PROBLEM_STARTED and event.started_at is not None:
                downtime_seconds += _overlap_seconds(_as_utc(event.started_at), now, window_start, now)
                break

    downtime_seconds = min(downtime_seconds, period_seconds)
    uptime_percent = 100.0 if period_seconds <= 0 else max(0.0, 100.0 * (1 - downtime_seconds / period_seconds))

    return UptimeResult(
        period_days=days,
        uptime_percent=round(uptime_percent, 2),
        downtime_seconds=int(downtime_seconds),
    )
