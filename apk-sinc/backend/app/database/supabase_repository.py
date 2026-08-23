"""Implementacao de Repository sobre Supabase (PostgreSQL), via supabase-py.

Esta e a implementacao usada em producao (db_backend=supabase). Os nomes de
tabela/coluna correspondem exatamente ao schema criado pelas migrations em
backend/supabase/migrations/.
"""

from datetime import date, datetime

from supabase import Client

from app.database.supabase_client import get_supabase_client
from app.models.domain import Device, Habit, HabitLog, Metric, Profile


def _parse_dt(value: str | None) -> datetime | None:
    if not value:
        return None
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def _iso(value: datetime | None) -> str | None:
    return value.isoformat() if value else None


def _profile_from_row(row: dict) -> Profile:
    return Profile(
        id=row.get("id"),
        display_name=row.get("display_name", "Voce"),
        created_at=_parse_dt(row.get("created_at")),
        updated_at=_parse_dt(row.get("updated_at")),
    )


def _habit_from_row(row: dict) -> Habit:
    return Habit(
        id=row.get("id"),
        profile_id=row["profile_id"],
        title=row["title"],
        category=row["category"],
        icon_key=row.get("icon_key", "circle"),
        target_value=row.get("target_value"),
        target_unit=row.get("target_unit"),
        color_tag=row.get("color_tag", "ACCENT"),
        active=row.get("active", True),
        sort_order=row.get("sort_order", 0),
        created_at=_parse_dt(row.get("created_at")),
        updated_at=_parse_dt(row.get("updated_at")),
    )


def _habit_log_from_row(row: dict) -> HabitLog:
    return HabitLog(
        id=row.get("id"),
        habit_id=row["habit_id"],
        log_date=date.fromisoformat(row["log_date"]),
        value=row.get("value"),
        completed=row.get("completed", False),
        logged_at=_parse_dt(row.get("logged_at")),
    )


def _metric_from_row(row: dict) -> Metric:
    return Metric(
        id=row.get("id"),
        profile_id=row["profile_id"],
        metric_type=row["metric_type"],
        value=row["value"],
        recorded_at=_parse_dt(row.get("recorded_at")),
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

    def get_or_create_profile(self, display_name: str = "Voce") -> Profile:
        result = self._client.table("profiles").select("*").limit(1).execute()
        if result.data:
            return _profile_from_row(result.data[0])
        created = self._client.table("profiles").insert({"display_name": display_name}).execute()
        return _profile_from_row(created.data[0])

    def update_profile(self, display_name: str) -> Profile:
        profile = self.get_or_create_profile(display_name)
        result = (
            self._client.table("profiles")
            .update({"display_name": display_name})
            .eq("id", profile.id)
            .execute()
        )
        return _profile_from_row(result.data[0])

    def list_habits(self, profile_id: str, active_only: bool = True) -> list[Habit]:
        query = self._client.table("habits").select("*").eq("profile_id", profile_id).order("sort_order")
        if active_only:
            query = query.eq("active", True)
        result = query.execute()
        return [_habit_from_row(r) for r in result.data]

    def get_habit(self, habit_id: str) -> Habit | None:
        result = self._client.table("habits").select("*").eq("id", habit_id).limit(1).execute()
        return _habit_from_row(result.data[0]) if result.data else None

    def upsert_habit(self, habit: Habit) -> Habit:
        payload = {
            "profile_id": habit.profile_id,
            "title": habit.title,
            "category": habit.category,
            "icon_key": habit.icon_key,
            "target_value": habit.target_value,
            "target_unit": habit.target_unit,
            "color_tag": habit.color_tag,
            "active": habit.active,
            "sort_order": habit.sort_order,
        }
        if habit.id:
            payload["id"] = habit.id
            result = self._client.table("habits").upsert(payload, on_conflict="id").execute()
        else:
            result = self._client.table("habits").insert(payload).execute()
        return _habit_from_row(result.data[0])

    def log_habit(self, habit_id: str, log_date: date, value: float | None, completed: bool) -> HabitLog:
        payload = {
            "habit_id": habit_id,
            "log_date": log_date.isoformat(),
            "value": value,
            "completed": completed,
            "logged_at": datetime.utcnow().isoformat(),
        }
        result = self._client.table("habit_logs").upsert(payload, on_conflict="habit_id,log_date").execute()
        return _habit_log_from_row(result.data[0])

    def get_habit_log(self, habit_id: str, log_date: date) -> HabitLog | None:
        result = (
            self._client.table("habit_logs")
            .select("*")
            .eq("habit_id", habit_id)
            .eq("log_date", log_date.isoformat())
            .limit(1)
            .execute()
        )
        return _habit_log_from_row(result.data[0]) if result.data else None

    def list_habit_logs(
        self, habit_id: str | None = None, since: date | None = None, limit: int = 200
    ) -> list[HabitLog]:
        query = self._client.table("habit_logs").select("*").order("log_date", desc=True).limit(limit)
        if habit_id:
            query = query.eq("habit_id", habit_id)
        if since:
            query = query.gte("log_date", since.isoformat())
        result = query.execute()
        return [_habit_log_from_row(r) for r in result.data]

    def add_metric(self, metric: Metric) -> Metric:
        payload = {
            "profile_id": metric.profile_id,
            "metric_type": metric.metric_type,
            "value": metric.value,
            "recorded_at": _iso(metric.recorded_at) or datetime.utcnow().isoformat(),
        }
        result = self._client.table("metrics").insert(payload).execute()
        return _metric_from_row(result.data[0])

    def list_metrics(
        self, profile_id: str, metric_type: str | None = None, since: date | None = None, limit: int = 100
    ) -> list[Metric]:
        query = (
            self._client.table("metrics")
            .select("*")
            .eq("profile_id", profile_id)
            .order("recorded_at", desc=True)
            .limit(limit)
        )
        if metric_type:
            query = query.eq("metric_type", metric_type)
        if since:
            query = query.gte("recorded_at", since.isoformat())
        result = query.execute()
        return [_metric_from_row(r) for r in result.data]

    def latest_metric(self, profile_id: str, metric_type: str) -> Metric | None:
        result = (
            self._client.table("metrics")
            .select("*")
            .eq("profile_id", profile_id)
            .eq("metric_type", metric_type)
            .order("recorded_at", desc=True)
            .limit(1)
            .execute()
        )
        return _metric_from_row(result.data[0]) if result.data else None

    def register_device(self, fcm_token: str, device_name: str | None = None) -> Device:
        now = datetime.utcnow().isoformat()
        payload = {"fcm_token": fcm_token, "device_name": device_name, "active": True, "last_seen_at": now}
        result = self._client.table("devices").upsert(payload, on_conflict="fcm_token").execute()
        return _device_from_row(result.data[0])

    def list_devices(self) -> list[Device]:
        result = self._client.table("devices").select("*").order("created_at").execute()
        return [_device_from_row(r) for r in result.data]

    def list_active_device_tokens(self) -> list[str]:
        result = self._client.table("devices").select("fcm_token").eq("active", True).execute()
        return [r["fcm_token"] for r in result.data]

    def health_check(self) -> bool:
        try:
            self._client.table("profiles").select("id").limit(1).execute()
            return True
        except Exception:
            return False
