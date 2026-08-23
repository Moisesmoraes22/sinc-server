"""Regra de status do monitor de sincronizacao.

Diferente da primeira versao do projeto (que contava falhas consecutivas de
"esta no ar / nao esta"), o dominio de sincronizacao classifica cada
unidade pelo TEMPO decorrido desde o ultimo envio/recebimento, comparado
contra limites configuraveis (`monitor_settings`):

    elapsed < warning_threshold_minutes                  -> NORMAL
    warning_threshold <= elapsed < critical_threshold     -> ATENCAO
    elapsed >= critical_threshold_minutes                  -> CRITICO

send_status e receive_status sao calculados independentemente (uma unidade
pode estar atrasada so no envio, so no recebimento, ou nos dois).
overall_status e o pior dos dois (CRITICO > ATENCAO > NORMAL).

Anti-spam: notifica-se SOMENTE quando overall_status muda de valor entre um
ciclo e outro (ex.: NORMAL -> ATENCAO, ATENCAO -> CRITICO, CRITICO ->
NORMAL). Repetir o mesmo status entre ciclos (ex.: CRITICO -> CRITICO)
nunca gera nova notificacao.
"""

from dataclasses import dataclass

from app.models.domain import ATENCAO, CRITICO, NORMAL

_SEVERITY = {NORMAL: 0, ATENCAO: 1, CRITICO: 2}


def compute_status(elapsed_minutes: float | None, warning_threshold: int, critical_threshold: int) -> str:
    """Classifica o status a partir do tempo decorrido (em minutos).

    `elapsed_minutes=None` (nunca houve envio/recebimento registrado) e
    tratado como CRITICO - unidade sem nenhum dado e o pior cenario possivel.
    """
    if elapsed_minutes is None:
        return CRITICO
    if elapsed_minutes >= critical_threshold:
        return CRITICO
    if elapsed_minutes >= warning_threshold:
        return ATENCAO
    return NORMAL


def compute_overall(send_status: str, receive_status: str) -> str:
    return max(send_status, receive_status, key=lambda status: _SEVERITY[status])


def should_notify(previous_overall: str, new_overall: str) -> bool:
    return previous_overall != new_overall


@dataclass
class StatusEvaluation:
    send_status: str
    receive_status: str
    overall_status: str
    previous_overall_status: str
    should_notify: bool


def evaluate(
    previous_overall_status: str,
    send_elapsed_minutes: float | None,
    receive_elapsed_minutes: float | None,
    warning_threshold_minutes: int,
    critical_threshold_minutes: int,
) -> StatusEvaluation:
    send_status = compute_status(send_elapsed_minutes, warning_threshold_minutes, critical_threshold_minutes)
    receive_status = compute_status(receive_elapsed_minutes, warning_threshold_minutes, critical_threshold_minutes)
    overall_status = compute_overall(send_status, receive_status)

    return StatusEvaluation(
        send_status=send_status,
        receive_status=receive_status,
        overall_status=overall_status,
        previous_overall_status=previous_overall_status,
        should_notify=should_notify(previous_overall_status, overall_status),
    )
