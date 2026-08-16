"""Implementacao de Repository sobre SQLite (via SQLAlchemy).

Uso: desenvolvimento local sem um projeto Supabase configurado, e base de
todos os testes automatizados (nunca dependem do Supabase real). O schema
espelha exatamente as tabelas Supabase (app/models/orm.py) para que o
comportamento observado nos testes seja o mesmo que em producao.
"""

from datetime import datetime

from sqlalchemy import select

from app.database.db import make_sessionmaker
from app.models.domain import Device, MonitorSettings, SyncEvent, SyncUnit
from app.models.orm import DeviceORM, MonitorSettingsORM, SyncEventORM, SyncUnitORM


def _unit_from_orm(row: SyncUnitORM) -> SyncUnit:
    return SyncUnit(
        id=row.id,
        a7_code=row.a7_code,
        business_unit=row.business_unit,
        revisions_to_send=row.revisions_to_send,
        last_send_at=row.last_send_at,
        last_receive_at=row.last_receive_at,
        send_elapsed_minutes=row.send_elapsed_minutes,
        receive_elapsed_minutes=row.receive_elapsed_minutes,
        send_status=row.send_status,
        receive_status=row.receive_status,
        overall_status=row.overall_status,
        consecutive_failures=row.consecutive_failures,
        last_checked_at=row.last_checked_at,
        created_at=row.created_at,
        updated_at=row.updated_at,
    )


def _event_from_orm(row: SyncEventORM) -> SyncEvent:
    return SyncEvent(
        id=row.id,
        unit_id=row.unit_id,
        a7_code=row.a7_code,
        business_unit=row.business_unit,
        event_type=row.event_type,
        direction=row.direction,
        previous_status=row.previous_status,
        new_status=row.new_status,
        started_at=row.started_at,
        resolved_at=row.resolved_at,
        duration_seconds=row.duration_seconds,
        message=row.message,
        created_at=row.created_at,
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

    def get_unit(self, a7_code: str) -> SyncUnit | None:
        with self._sessionmaker() as session:
            row = session.scalar(select(SyncUnitORM).where(SyncUnitORM.a7_code == a7_code))
            return _unit_from_orm(row) if row else None

    def list_units(self) -> list[SyncUnit]:
        with self._sessionmaker() as session:
            rows = session.scalars(select(SyncUnitORM).order_by(SyncUnitORM.business_unit))
            return [_unit_from_orm(r) for r in rows]

    def upsert_unit(self, unit: SyncUnit) -> SyncUnit:
        with self._sessionmaker() as session:
            row = session.scalar(select(SyncUnitORM).where(SyncUnitORM.a7_code == unit.a7_code))
            if row is None:
                row = SyncUnitORM(a7_code=unit.a7_code)
                session.add(row)

            row.business_unit = unit.business_unit
            row.revisions_to_send = unit.revisions_to_send
            row.last_send_at = unit.last_send_at
            row.last_receive_at = unit.last_receive_at
            row.send_elapsed_minutes = unit.send_elapsed_minutes
            row.receive_elapsed_minutes = unit.receive_elapsed_minutes
            row.send_status = unit.send_status
            row.receive_status = unit.receive_status
            row.overall_status = unit.overall_status
            row.consecutive_failures = unit.consecutive_failures
            row.last_checked_at = unit.last_checked_at
            row.updated_at = datetime.utcnow()

            session.commit()
            session.refresh(row)
            return _unit_from_orm(row)

    def add_event(self, event: SyncEvent) -> SyncEvent:
        with self._sessionmaker() as session:
            row = SyncEventORM(
                unit_id=event.unit_id,
                a7_code=event.a7_code,
                business_unit=event.business_unit,
                event_type=event.event_type,
                direction=event.direction,
                previous_status=event.previous_status,
                new_status=event.new_status,
                started_at=event.started_at,
                resolved_at=event.resolved_at,
                duration_seconds=event.duration_seconds,
                message=event.message,
            )
            session.add(row)
            session.commit()
            session.refresh(row)
            return _event_from_orm(row)

    def list_events(
        self, unit_a7_code: str | None = None, status: str | None = None, limit: int = 200
    ) -> list[SyncEvent]:
        with self._sessionmaker() as session:
            stmt = select(SyncEventORM).order_by(SyncEventORM.created_at.desc()).limit(limit)
            if unit_a7_code:
                stmt = stmt.where(SyncEventORM.a7_code == unit_a7_code)
            if status:
                stmt = stmt.where(SyncEventORM.new_status == status)
            rows = session.scalars(stmt)
            return [_event_from_orm(r) for r in rows]

    def get_last_event_for_unit(self, a7_code: str) -> SyncEvent | None:
        with self._sessionmaker() as session:
            row = session.scalar(
                select(SyncEventORM)
                .where(SyncEventORM.a7_code == a7_code)
                .order_by(SyncEventORM.created_at.desc())
                .limit(1)
            )
            return _event_from_orm(row) if row else None

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

    def get_settings(self) -> MonitorSettings:
        with self._sessionmaker() as session:
            row = session.get(MonitorSettingsORM, 1)
            if row is None:
                row = MonitorSettingsORM(id=1)
                session.add(row)
                session.commit()
                session.refresh(row)
            return MonitorSettings(
                warning_threshold_minutes=row.warning_threshold_minutes,
                critical_threshold_minutes=row.critical_threshold_minutes,
                check_interval_seconds=row.check_interval_seconds,
            )

    def update_settings(self, settings: MonitorSettings) -> MonitorSettings:
        with self._sessionmaker() as session:
            row = session.get(MonitorSettingsORM, 1)
            if row is None:
                row = MonitorSettingsORM(id=1)
                session.add(row)
            row.warning_threshold_minutes = settings.warning_threshold_minutes
            row.critical_threshold_minutes = settings.critical_threshold_minutes
            row.check_interval_seconds = settings.check_interval_seconds
            session.commit()
            session.refresh(row)
            return settings

    def health_check(self) -> bool:
        try:
            with self._sessionmaker() as session:
                session.execute(select(1))
            return True
        except Exception:
            return False
