from datetime import datetime

from sqlalchemy import DateTime, Integer, String
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


class ServerORM(Base):
    __tablename__ = "servers"

    id: Mapped[str] = mapped_column(String, primary_key=True)
    name: Mapped[str] = mapped_column(String)
    status: Mapped[str] = mapped_column(String, default="ONLINE")
    consecutive_failures: Mapped[int] = mapped_column(Integer, default=0)
    response_time_ms: Mapped[int | None] = mapped_column(Integer, nullable=True)
    last_check: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    last_down_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    last_up_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    down_count: Mapped[int] = mapped_column(Integer, default=0)


class EventORM(Base):
    __tablename__ = "events"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    server_id: Mapped[str] = mapped_column(String, index=True)
    server_name: Mapped[str] = mapped_column(String)
    from_status: Mapped[str] = mapped_column(String)
    to_status: Mapped[str] = mapped_column(String)
    occurred_at: Mapped[datetime] = mapped_column(DateTime)
    reason: Mapped[str | None] = mapped_column(String, nullable=True)
    response_time_ms: Mapped[int | None] = mapped_column(Integer, nullable=True)


class DeviceTokenORM(Base):
    __tablename__ = "device_tokens"

    token: Mapped[str] = mapped_column(String, primary_key=True)
    platform: Mapped[str] = mapped_column(String, default="android")
    registered_at: Mapped[datetime] = mapped_column(DateTime)
