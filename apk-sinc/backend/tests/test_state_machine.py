from app.models.domain import ATENCAO, CRITICO, NORMAL
from app.services.state_machine import compute_overall, compute_status, evaluate, should_notify

WARNING = 5
CRITICAL = 15


def test_elapsed_below_warning_is_normal():
    assert compute_status(2, WARNING, CRITICAL) == NORMAL


def test_elapsed_at_warning_threshold_is_attention():
    assert compute_status(5, WARNING, CRITICAL) == ATENCAO
    assert compute_status(10, WARNING, CRITICAL) == ATENCAO


def test_elapsed_at_critical_threshold_is_critical():
    assert compute_status(15, WARNING, CRITICAL) == CRITICO
    assert compute_status(60, WARNING, CRITICAL) == CRITICO


def test_no_data_ever_is_critical():
    assert compute_status(None, WARNING, CRITICAL) == CRITICO


def test_overall_is_the_worst_of_send_and_receive():
    assert compute_overall(NORMAL, NORMAL) == NORMAL
    assert compute_overall(NORMAL, ATENCAO) == ATENCAO
    assert compute_overall(ATENCAO, NORMAL) == ATENCAO
    assert compute_overall(ATENCAO, CRITICO) == CRITICO
    assert compute_overall(CRITICO, NORMAL) == CRITICO


def test_should_notify_only_on_change():
    assert should_notify(NORMAL, NORMAL) is False
    assert should_notify(NORMAL, ATENCAO) is True
    assert should_notify(ATENCAO, ATENCAO) is False
    assert should_notify(ATENCAO, CRITICO) is True
    assert should_notify(CRITICO, CRITICO) is False
    assert should_notify(CRITICO, NORMAL) is True


def test_evaluate_builds_full_result():
    result = evaluate(
        previous_overall_status=NORMAL,
        send_elapsed_minutes=20,
        receive_elapsed_minutes=1,
        warning_threshold_minutes=WARNING,
        critical_threshold_minutes=CRITICAL,
    )
    assert result.send_status == CRITICO
    assert result.receive_status == NORMAL
    assert result.overall_status == CRITICO
    assert result.should_notify is True


def test_evaluate_no_notification_when_status_repeats():
    result = evaluate(
        previous_overall_status=CRITICO,
        send_elapsed_minutes=30,
        receive_elapsed_minutes=30,
        warning_threshold_minutes=WARNING,
        critical_threshold_minutes=CRITICAL,
    )
    assert result.overall_status == CRITICO
    assert result.should_notify is False
