from datetime import datetime

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.orm import DeviceTokenORM, EventORM, ServerORM


class ServerRepository:
    def __init__(self, session: Session):
        self.session = session

    def get(self, server_id: str) -> ServerORM | None:
        return self.session.get(ServerORM, server_id)

    def list_all(self) -> list[ServerORM]:
        return list(self.session.scalars(select(ServerORM).order_by(ServerORM.name)))

    def upsert(self, server: ServerORM) -> ServerORM:
        existing = self.session.get(ServerORM, server.id)
        if existing is None:
            self.session.add(server)
            self.session.flush()
            return server
        existing.name = server.name
        existing.status = server.status
        existing.consecutive_failures = server.consecutive_failures
        existing.response_time_ms = server.response_time_ms
        existing.last_check = server.last_check
        if server.last_down_at is not None:
            existing.last_down_at = server.last_down_at
        if server.last_up_at is not None:
            existing.last_up_at = server.last_up_at
        existing.down_count = server.down_count
        self.session.flush()
        return existing

    def commit(self) -> None:
        self.session.commit()


class EventRepository:
    def __init__(self, session: Session):
        self.session = session

    def add(self, event: EventORM) -> EventORM:
        self.session.add(event)
        self.session.flush()
        return event

    def list_all(
        self, server_id: str | None = None, status: str | None = None, limit: int = 200
    ) -> list[EventORM]:
        stmt = select(EventORM).order_by(EventORM.occurred_at.desc()).limit(limit)
        if server_id:
            stmt = stmt.where(EventORM.server_id == server_id)
        if status:
            stmt = stmt.where(EventORM.to_status == status)
        return list(self.session.scalars(stmt))

    def commit(self) -> None:
        self.session.commit()


class DeviceTokenRepository:
    def __init__(self, session: Session):
        self.session = session

    def register(self, token: str, platform: str = "android") -> DeviceTokenORM:
        existing = self.session.get(DeviceTokenORM, token)
        if existing:
            return existing
        device = DeviceTokenORM(token=token, platform=platform, registered_at=datetime.utcnow())
        self.session.add(device)
        self.session.flush()
        self.session.commit()
        return device

    def list_tokens(self) -> list[str]:
        return list(self.session.scalars(select(DeviceTokenORM.token)))
