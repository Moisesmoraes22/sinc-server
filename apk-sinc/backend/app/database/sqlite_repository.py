"""Implementacao de Repository sobre SQLite (via SQLAlchemy).

Uso: desenvolvimento local sem um projeto Supabase configurado, e base de
todos os testes automatizados (nunca dependem do Supabase real). O schema
espelha exatamente as tabelas Supabase (app/models/orm.py) para que o
comportamento observado nos testes seja o mesmo que em producao.
"""

from datetime import date, datetime

from sqlalchemy import select

from app.database.db import make_sessionmaker
from app.models.domain import Device, Habit, HabitLog, Metric, Profile
from app.models.orm import DeviceORM, HabitLogORM, HabitORM, MetricORM, ProfileORM


def _profile_from_orm(row: ProfileORM) -> Profile:
    return Profile(
        id=row.id,
        display_name=row.display_name,
        created_at=row.created_at,
        updated_at=row.updated_at,
    )


def _habit_from_orm(row: HabitORM) -> Habit:
    return Habit(
        id=row.id,
        profile_id=row.profile_id,
        title=row.title,
        category=row.category,
        icon_key=row.icon_key,
        target_value=row.target_value,
        target_unit=row.target_unit,
        color_tag=row.color_tag,
        active=row.active,
        sort_order=row.sort_order,
        created_at=row.created_at,
        updated_at=row.updated_at,
    )


def _habit_log_from_orm(row: HabitLogORM) -> HabitLog:
    return HabitLog(
        id=row.id,
        habit_id=row.habit_id,
        log_date=row.log_date,
        value=row.value,
        completed=row.completed,
        logged_at=row.logged_at,
    )


def _metric_from_orm(row: MetricORM) -> Metric:
    return Metric(
        id=row.id,
        profile_id=row.profile_id,
        metric_type=row.metric_type,
        value=row.value,
        recorded_at=row.recorded_at,
    )


def _device_from_orm(row: DeviceORM) -> Device:
    return Device(
        id=row.id,
        device_name=row.device_name,
        fcm_token=row.fcm_token,
        active=row.active,
        created_at=row.created_at,
        updated_at=row.updated_at,
        last_seen_at=row.last_seen_at,
    )


class SqliteRepository:
    def __init__(self, database_url: str = "sqlite:///./sinc.db"):
        self._sessionmaker = make_sessionmaker(database_url)

    def get_or_create_profile(self, display_name: str = "Voce") -> Profile:
        with self._sessionmaker() as session:
            row = session.scalar(select(ProfileORM).limit(1))
            if row is None:
                row = ProfileORM(display_name=display_name)
                session.add(row)
                session.commit()
                session.refresh(row)
            return _profile_from_orm(row)

    def update_profile(self, display_name: str) -> Profile:
        with self._sessionmaker() as session:
            row = session.scalar(select(ProfileORM).limit(1))
            if row is None:
                row = ProfileORM(display_name=display_name)
                session.add(row)
            else:
                row.display_name = display_name
                row.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(row)
            return _profile_from_orm(row)

    def list_habits(self, profile_id: str, active_only: bool = True) -> list[Habit]:
        with self._sessionmaker() as session:
            stmt = select(HabitORM).where(HabitORM.profile_id == profile_id).order_by(HabitORM.sort_order)
            if active_only:
                stmt = stmt.where(HabitORM.active.is_(True))
            rows = session.scalars(stmt)
            return [_habit_from_orm(r) for r in rows]

    def get_habit(self, habit_id: str) -> Habit | None:
        with self._sessionmaker() as session:
            row = session.get(HabitORM, habit_id)
            return _habit_from_orm(row) if row else None

    def upsert_habit(self, habit: Habit) -> Habit:
        with self._sessionmaker() as session:
            row = session.get(HabitORM, habit.id) if habit.id else None
            if row is None:
                row = HabitORM(profile_id=habit.profile_id)
                session.add(row)

            row.title = habit.title
            row.category = habit.category
            row.icon_key = habit.icon_key
            row.target_value = habit.target_value
            row.target_unit = habit.target_unit
            row.color_tag = habit.color_tag
            row.active = habit.active
            row.sort_order = habit.sort_order
            row.updated_at = datetime.utcnow()

            session.commit()
            session.refresh(row)
            return _habit_from_orm(row)

    def log_habit(self, habit_id: str, log_date: date, value: float | None, completed: bool) -> HabitLog:
        with self._sessionmaker() as session:
            row = session.scalar(
                select(HabitLogORM).where(HabitLogORM.habit_id == habit_id, HabitLogORM.log_date == log_date)
            )
            if row is None:
                row = HabitLogORM(habit_id=habit_id, log_date=log_date)
                session.add(row)
            row.value = value
            row.completed = completed
            row.logged_at = datetime.utcnow()
            session.commit()
            session.refresh(row)
            return _habit_log_from_orm(row)

    def get_habit_log(self, habit_id: str, log_date: date) -> HabitLog | None:
        with self._sessionmaker() as session:
            row = session.scalar(
                select(HabitLogORM).where(HabitLogORM.habit_id == habit_id, HabitLogORM.log_date == log_date)
            )
            return _habit_log_from_orm(row) if row else None

    def list_habit_logs(
        self, habit_id: str | None = None, since: date | None = None, limit: int = 200
    ) -> list[HabitLog]:
        with self._sessionmaker() as session:
            stmt = select(HabitLogORM).order_by(HabitLogORM.log_date.desc()).limit(limit)
            if habit_id:
                stmt = stmt.where(HabitLogORM.habit_id == habit_id)
            if since:
                stmt = stmt.where(HabitLogORM.log_date >= since)
            rows = session.scalars(stmt)
            return [_habit_log_from_orm(r) for r in rows]

    def add_metric(self, metric: Metric) -> Metric:
        with self._sessionmaker() as session:
            row = MetricORM(
                profile_id=metric.profile_id,
                metric_type=metric.metric_type,
                value=metric.value,
                recorded_at=metric.recorded_at or datetime.utcnow(),
            )
            session.add(row)
            session.commit()
            session.refresh(row)
            return _metric_from_orm(row)

    def list_metrics(
        self, profile_id: str, metric_type: str | None = None, since: date | None = None, limit: int = 100
    ) -> list[Metric]:
        with self._sessionmaker() as session:
            stmt = (
                select(MetricORM)
                .where(MetricORM.profile_id == profile_id)
                .order_by(MetricORM.recorded_at.desc())
                .limit(limit)
            )
            if metric_type:
                stmt = stmt.where(MetricORM.metric_type == metric_type)
            if since:
                stmt = stmt.where(MetricORM.recorded_at >= since)
            rows = session.scalars(stmt)
            return [_metric_from_orm(r) for r in rows]

    def latest_metric(self, profile_id: str, metric_type: str) -> Metric | None:
        with self._sessionmaker() as session:
            row = session.scalar(
                select(MetricORM)
                .where(MetricORM.profile_id == profile_id, MetricORM.metric_type == metric_type)
                .order_by(MetricORM.recorded_at.desc())
                .limit(1)
            )
            return _metric_from_orm(row) if row else None

    def register_device(self, fcm_token: str, device_name: str | None = None) -> Device:
        with self._sessionmaker() as session:
            row = session.scalar(select(DeviceORM).where(DeviceORM.fcm_token == fcm_token))
            now = datetime.utcnow()
            if row is None:
                row = DeviceORM(fcm_token=fcm_token, device_name=device_name, active=True, last_seen_at=now)
                session.add(row)
            else:
                row.device_name = device_name or row.device_name
                row.active = True
                row.last_seen_at = now
                row.updated_at = now
            session.commit()
            session.refresh(row)
            return _device_from_orm(row)

    def list_devices(self) -> list[Device]:
        with self._sessionmaker() as session:
            rows = session.scalars(select(DeviceORM).order_by(DeviceORM.created_at))
            return [_device_from_orm(r) for r in rows]

    def list_active_device_tokens(self) -> list[str]:
        with self._sessionmaker() as session:
            rows = session.scalars(select(DeviceORM.fcm_token).where(DeviceORM.active.is_(True)))
            return list(rows)

    def health_check(self) -> bool:
        try:
            with self._sessionmaker() as session:
                session.execute(select(1))
            return True
        except Exception:
            return False
