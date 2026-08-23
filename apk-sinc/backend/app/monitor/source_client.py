from typing import Protocol

from app.models.domain import SyncUnitReading


class SourceClient(Protocol):
    """Interface comum entre a fonte real e a fonte simulada (mock)."""

    async def fetch_units(self) -> list[SyncUnitReading]: ...
