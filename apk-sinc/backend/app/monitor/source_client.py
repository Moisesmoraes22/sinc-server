from typing import Protocol

from app.models.schemas import ServerReading


class SourceClient(Protocol):
    """Interface comum entre a fonte real e a fonte simulada (mock)."""

    async def fetch_servers(self) -> list[ServerReading]:
        ...
