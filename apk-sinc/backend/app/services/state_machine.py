"""Regra de transicao de status e anti-spam.

ONLINE --(falha)--> ATENCAO --(falha)--> ATENCAO --(falha)--> OFFLINE
OFFLINE --(sucesso)--> ONLINE
ATENCAO --(sucesso)--> ONLINE

Notificacao (FCM) e disparada SOMENTE nas transicoes:
  * qualquer coisa -> OFFLINE (perda real, apos threshold de falhas)
  * OFFLINE -> ONLINE (recuperacao)

Entrar em ATENCAO nao notifica. Isso e o mecanismo anti-spam: uma sequencia
de N falhas consecutivas gera OFFLINE (e notificacao) apenas uma vez, na
falha que cruza o threshold; falhas subsequentes enquanto ja OFFLINE nao
geram novas notificacoes ate que o servidor volte a responder.
"""

from dataclasses import dataclass
from datetime import datetime

from app.config import get_settings

STATUS_ONLINE = "ONLINE"
STATUS_ATTENTION = "ATENCAO"
STATUS_OFFLINE = "OFFLINE"


@dataclass
class ServerState:
    status: str
    consecutive_failures: int


@dataclass
class Transition:
    previous_status: str
    new_status: str
    consecutive_failures: int
    should_notify: bool
    reason: str | None = None


def evaluate(
    current: ServerState,
    is_up: bool,
    failure_threshold_attention: int | None = None,
    failure_threshold_offline: int | None = None,
) -> Transition:
    settings = get_settings()
    attention_threshold = failure_threshold_attention or settings.failure_threshold_attention
    offline_threshold = failure_threshold_offline or settings.failure_threshold_offline

    if is_up:
        new_status = STATUS_ONLINE
        new_failures = 0
        should_notify = current.status == STATUS_OFFLINE
        reason = "Servidor voltou a responder" if should_notify else None
        return Transition(current.status, new_status, new_failures, should_notify, reason)

    new_failures = current.consecutive_failures + 1

    if new_failures >= offline_threshold:
        new_status = STATUS_OFFLINE
    elif new_failures >= attention_threshold:
        new_status = STATUS_ATTENTION
    else:
        new_status = current.status

    should_notify = new_status == STATUS_OFFLINE and current.status != STATUS_OFFLINE
    reason = (
        f"{new_failures} falhas consecutivas detectadas"
        if should_notify
        else None
    )
    return Transition(current.status, new_status, new_failures, should_notify, reason)
