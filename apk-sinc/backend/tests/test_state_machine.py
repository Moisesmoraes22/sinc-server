from app.services.state_machine import ServerState, evaluate


def test_first_failure_goes_to_attention():
    state = ServerState(status="ONLINE", consecutive_failures=0)
    t = evaluate(state, is_up=False)
    assert t.new_status == "ATENCAO"
    assert t.consecutive_failures == 1
    assert t.should_notify is False


def test_second_failure_stays_attention():
    state = ServerState(status="ATENCAO", consecutive_failures=1)
    t = evaluate(state, is_up=False)
    assert t.new_status == "ATENCAO"
    assert t.consecutive_failures == 2
    assert t.should_notify is False


def test_third_consecutive_failure_goes_offline_and_notifies():
    state = ServerState(status="ATENCAO", consecutive_failures=2)
    t = evaluate(state, is_up=False)
    assert t.new_status == "OFFLINE"
    assert t.consecutive_failures == 3
    assert t.should_notify is True


def test_recovery_from_offline_notifies():
    state = ServerState(status="OFFLINE", consecutive_failures=3)
    t = evaluate(state, is_up=True)
    assert t.new_status == "ONLINE"
    assert t.consecutive_failures == 0
    assert t.should_notify is True


def test_recovery_from_attention_does_not_notify():
    state = ServerState(status="ATENCAO", consecutive_failures=1)
    t = evaluate(state, is_up=True)
    assert t.new_status == "ONLINE"
    assert t.should_notify is False


def test_repeated_offline_failures_do_not_renotify():
    """Anti-spam: uma vez OFFLINE, novas falhas nao devem gerar nova notificacao."""
    state = ServerState(status="OFFLINE", consecutive_failures=5)
    t = evaluate(state, is_up=False)
    assert t.new_status == "OFFLINE"
    assert t.should_notify is False


def test_already_online_success_does_not_notify():
    state = ServerState(status="ONLINE", consecutive_failures=0)
    t = evaluate(state, is_up=True)
    assert t.new_status == "ONLINE"
    assert t.should_notify is False


def test_full_flapping_then_recovery_sequence():
    """ONLINE, OFFLINE, OFFLINE, OFFLINE, OFFLINE, OFFLINE -> so uma notificacao de queda."""
    state = ServerState(status="ONLINE", consecutive_failures=0)
    notifications = []
    for is_up in [False, False, False, False, False]:
        t = evaluate(state, is_up)
        if t.should_notify:
            notifications.append(t.new_status)
        state = ServerState(status=t.new_status, consecutive_failures=t.consecutive_failures)

    assert notifications == ["OFFLINE"]
    assert state.status == "OFFLINE"

    # volta a responder -> uma notificacao de retorno
    t = evaluate(state, is_up=True)
    assert t.should_notify is True
    assert t.new_status == "ONLINE"

    # cai novamente -> deve notificar de novo apos atingir o threshold
    state = ServerState(status=t.new_status, consecutive_failures=t.consecutive_failures)
    notifications2 = []
    for is_up in [False, False, False]:
        t = evaluate(state, is_up)
        if t.should_notify:
            notifications2.append(t.new_status)
        state = ServerState(status=t.new_status, consecutive_failures=t.consecutive_failures)
    assert notifications2 == ["OFFLINE"]


def test_configurable_thresholds():
    state = ServerState(status="ONLINE", consecutive_failures=0)
    t = evaluate(state, is_up=False, failure_threshold_attention=1, failure_threshold_offline=1)
    assert t.new_status == "OFFLINE"
    assert t.should_notify is True
