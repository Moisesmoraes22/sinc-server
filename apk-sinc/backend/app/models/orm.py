"""Modelos SQLAlchemy usados APENAS pelo SqliteRepository (dev local sem
Supabase configurado / testes automatizados). O schema espelha as tabelas
Supabase definidas em backend/supabase/migrations/, para que a logica de
negocio se comporte de forma identica em ambos os backends.
"""

import uuid
from datetime import datetime

from sqlalchemy import Boolean, DateTime, Float, ForeignKey, Integer, String, Text
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


def _uuid() -> str:
    return str(uuid.uuid4())


class SyncUnitORM(Base):
    __tablename__ = "sync_units"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=_uuid)
    a7_code: Mapped[str] = mapped_column(String, unique=True, index=True)
    business_unit: Mapped[str] = mapped_column(String, index=True)
    revisions_to_send: Mapped[int | None] = mapped_column(Integer, nullable=True)
    last_send_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    last_receive_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    send_elapsed_minutes: Mapped[float | None] = mapped_column(Float, nullable=True)
    receive_elapsed_minutes: Mapped[float | None] = mapped_column(Float, nullable=True)
    send_status: Mapped[str] = mapped_column(String, default="NORMAL")
    receive_status: Mapped[str] = mapped_column(String, default="NORMAL")
    overall_status: Mapped[str] = mapped_column(String, default="NORMAL", index=True)
    consecutive_failures: Mapped[int] = mapped_column(Integer, default=0)
    last_checked_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class SyncEventORM(Base):
    __tablename__ = "sync_events"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    unit_id: Mapped[str | None] = mapped_column(String, ForeignKey("sync_units.id"), nullable=True, index=True)
    a7_code: Mapped[str] = mapped_column(String, index=True)
    business_unit: Mapped[str] = mapped_column(String)
    event_type: Mapped[str] = mapped_column(String)
    direction: Mapped[str] = mapped_column(String)
    previous_status: Mapped[str | None] = mapped_column(String, nullable=True)
    new_status: Mapped[str] = mapped_column(String, index=True)
    started_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    resolved_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    duration_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)
    message: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, index=True)


class DeviceORM(Base):
    __tablename__ = "devices"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=_uuid)
    device_name: Mapped[str | None] = mapped_column(String, nullable=True)
    fcm_token: Mapped[str] = mapped_column(String, unique=True, index=True)
    active: Mapped[bool] = mapped_column(Boolean, default=True, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    last_seen_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)


class MonitorSettingsORM(Base):
    __tablename__ = "monitor_settings"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, default=1)
    warning_threshold_minutes: Mapped[int] = mapped_column(Integer, default=5)
    critical_threshold_minutes: Mapped[int] = mapped_column(Integer, default=15)
    check_interval_seconds: Mapped[int] = mapped_column(Integer, default=60)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
