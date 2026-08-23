"""Regras de negocio do dominio de habitos/metricas - a Home do OMG SINC
precisa responder em uma unica chamada "como estou indo hoje", entao esta
camada agrega dados de varias tabelas (habitos, logs do dia, metricas,
progresso da semana) em algo pronto para a tela consumir.

Mantido deliberadamente simples: o app e de um unico usuario por instalacao
(sem multi-tenant/autenticacao ainda - mesmo padrao do antigo
monitor_settings singleton), entao nao ha filtro de "dono" alem do
profile_id already resolvido no router.
"""

from datetime import date, timedelta

from app.database.base import Repository
from app.models.domain import VALID_METRIC_TYPES, Habit, HabitLog, Profile
from app.models.schemas import HabitOut, HomeMetricSnapshot, HomeSummaryResponse

_NEXT_ACTION_ALL_DONE = "Todos os habitos de hoje estao concluidos. Bom trabalho!"
_NEXT_ACTION_NONE_CONFIGURED = "Adicione seu primeiro habito para comecar a acompanhar seu dia."


def habit_to_out(habit: Habit, today_log: HabitLog | None) -> HabitOut:
    return HabitOut(
        id=habit.id,
        title=habit.title,
        category=habit.category,
        icon_key=habit.icon_key,
        target_value=habit.target_value,
        target_unit=habit.target_unit,
        color_tag=habit.color_tag,
        today_value=today_log.value if today_log else None,
        today_completed=today_log.completed if today_log else False,
    )


def _weekly_completion_rate(logs: list[HabitLog], habits_total: int, start: date, end: date) -> float:
    if habits_total == 0:
        return 0.0
    days = (end - start).days + 1
    possible = habits_total * days
    completed = sum(1 for log in logs if log.completed and start <= log.log_date <= end)
    return round((completed / possible) * 100, 1) if possible else 0.0


def compute_home_summary(repository: Repository, profile: Profile) -> HomeSummaryResponse:
    today = date.today()
    habits = repository.list_habits(profile.id)

    today_logs_by_habit = {}
    for habit in habits:
        today_logs_by_habit[habit.id] = repository.get_habit_log(habit.id, today)

    habit_outs = [habit_to_out(h, today_logs_by_habit.get(h.id)) for h in habits]
    habits_done_today = sum(1 for h in habit_outs if h.today_completed)

    # janela de 14 dias cobre "esta semana" (0-6) e "semana passada" (7-13),
    # usada so para o delta de progresso mostrado na Home.
    since = today - timedelta(days=13)
    recent_logs = repository.list_habit_logs(since=since, limit=1000)
    habit_ids = {h.id for h in habits}
    recent_logs = [log for log in recent_logs if log.habit_id in habit_ids]

    this_week_start = today - timedelta(days=6)
    last_week_start = today - timedelta(days=13)
    last_week_end = today - timedelta(days=7)

    this_week_pct = _weekly_completion_rate(recent_logs, len(habits), this_week_start, today)
    last_week_pct = _weekly_completion_rate(recent_logs, len(habits), last_week_start, last_week_end)
    delta_pct = round(this_week_pct - last_week_pct, 1)

    next_action_habit = next((h for h in habit_outs if not h.today_completed), None)
    if not habit_outs:
        next_action_message = _NEXT_ACTION_NONE_CONFIGURED
        next_action_id = None
    elif next_action_habit is None:
        next_action_message = _NEXT_ACTION_ALL_DONE
        next_action_id = None
    else:
        next_action_message = f"Que tal registrar \"{next_action_habit.title}\" agora?"
        next_action_id = next_action_habit.id

    metrics = []
    for metric_type in VALID_METRIC_TYPES:
        latest = repository.latest_metric(profile.id, metric_type)
        metrics.append(
            HomeMetricSnapshot(
                metric_type=metric_type,
                value=latest.value if latest else None,
                recorded_at=latest.recorded_at if latest else None,
            )
        )

    return HomeSummaryResponse(
        greeting_name=profile.display_name,
        weekly_progress_pct=this_week_pct,
        weekly_progress_delta_pct=delta_pct,
        habits_done_today=habits_done_today,
        habits_total_today=len(habit_outs),
        next_action_habit_id=next_action_id,
        next_action_message=next_action_message,
        metrics=metrics,
        habits=habit_outs,
    )
