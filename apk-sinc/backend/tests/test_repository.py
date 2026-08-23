from datetime import date, timedelta

from app.models.domain import CATEGORY_AGUA, COLOR_ACCENT, METRIC_MOOD_SCORE, Habit, Metric


def test_get_or_create_profile_is_idempotent(repository):
    first = repository.get_or_create_profile("Ana")
    second = repository.get_or_create_profile("outro nome ignorado")
    assert first.id == second.id
    assert second.display_name == "Ana"


def test_update_profile(repository):
    repository.get_or_create_profile("Ana")
    updated = repository.update_profile("Ana Paula")
    assert updated.display_name == "Ana Paula"


def test_upsert_habit_creates_and_updates(repository):
    profile = repository.get_or_create_profile("Ana")
    habit = Habit(
        profile_id=profile.id,
        title="Beber agua",
        category=CATEGORY_AGUA,
        target_value=2.5,
        target_unit="L",
        color_tag=COLOR_ACCENT,
    )
    saved = repository.upsert_habit(habit)
    assert saved.id is not None
    assert saved.title == "Beber agua"

    saved.title = "Beber mais agua"
    updated = repository.upsert_habit(saved)
    assert updated.title == "Beber mais agua"
    assert updated.id == saved.id


def test_list_habits_orders_and_filters_active(repository):
    profile = repository.get_or_create_profile("Ana")
    h1 = repository.upsert_habit(Habit(profile_id=profile.id, title="B", category=CATEGORY_AGUA, sort_order=1))
    repository.upsert_habit(Habit(profile_id=profile.id, title="A", category=CATEGORY_AGUA, sort_order=0))
    inactive = repository.upsert_habit(Habit(profile_id=profile.id, title="C", category=CATEGORY_AGUA, sort_order=2, active=False))

    habits = repository.list_habits(profile.id)
    assert [h.title for h in habits] == ["A", "B"]
    assert inactive.id not in [h.id for h in habits]


def test_log_habit_is_idempotent_per_day(repository):
    profile = repository.get_or_create_profile("Ana")
    habit = repository.upsert_habit(Habit(profile_id=profile.id, title="Beber agua", category=CATEGORY_AGUA))
    today = date.today()

    repository.log_habit(habit.id, today, value=1.0, completed=False)
    log = repository.log_habit(habit.id, today, value=2.5, completed=True)

    assert log.value == 2.5
    assert log.completed is True

    fetched = repository.get_habit_log(habit.id, today)
    assert fetched.value == 2.5

    all_logs = repository.list_habit_logs(habit_id=habit.id)
    assert len(all_logs) == 1  # upsert por dia, nao duplica


def test_list_habit_logs_filters_since(repository):
    profile = repository.get_or_create_profile("Ana")
    habit = repository.upsert_habit(Habit(profile_id=profile.id, title="Beber agua", category=CATEGORY_AGUA))
    today = date.today()
    repository.log_habit(habit.id, today - timedelta(days=10), value=1.0, completed=True)
    repository.log_habit(habit.id, today, value=1.0, completed=True)

    recent = repository.list_habit_logs(habit_id=habit.id, since=today - timedelta(days=3))
    assert len(recent) == 1
    assert recent[0].log_date == today


def test_add_and_list_metrics(repository):
    profile = repository.get_or_create_profile("Ana")
    repository.add_metric(Metric(profile_id=profile.id, metric_type=METRIC_MOOD_SCORE, value=8))
    repository.add_metric(Metric(profile_id=profile.id, metric_type=METRIC_MOOD_SCORE, value=9))

    metrics = repository.list_metrics(profile.id, metric_type=METRIC_MOOD_SCORE)
    assert len(metrics) == 2

    latest = repository.latest_metric(profile.id, METRIC_MOOD_SCORE)
    assert latest.value == 9


def test_latest_metric_returns_none_when_missing(repository):
    profile = repository.get_or_create_profile("Ana")
    assert repository.latest_metric(profile.id, METRIC_MOOD_SCORE) is None


def test_register_device_is_idempotent(repository):
    first = repository.register_device("token-abc", device_name="android")
    second = repository.register_device("token-abc", device_name="android")
    assert first.id == second.id
    assert len(repository.list_devices()) == 1


def test_list_active_device_tokens(repository):
    repository.register_device("token-1")
    repository.register_device("token-2")
    tokens = repository.list_active_device_tokens()
    assert set(tokens) == {"token-1", "token-2"}


def test_health_check_returns_true_when_reachable(repository):
    assert repository.health_check() is True
