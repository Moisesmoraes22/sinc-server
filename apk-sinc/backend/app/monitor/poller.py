import asyncio
import logging

from app.config import get_settings
from app.database.db import get_session
from app.services.monitor_service import MonitorService

logger = logging.getLogger("sinc.poller")


class Poller:
    """Loop assincrono que roda o ciclo de checagem periodicamente, com retry."""

    def __init__(self, monitor_service: MonitorService):
        self.monitor_service = monitor_service
        self.settings = get_settings()
        self._task: asyncio.Task | None = None
        self._stop = asyncio.Event()
        self.last_poll_at = None

    async def _run_cycle_with_retry(self) -> None:
        attempts = max(1, self.settings.retry_attempts)
        for attempt in range(1, attempts + 1):
            session = get_session()
            try:
                await self.monitor_service.run_check_cycle(session)
                return
            except Exception:
                logger.exception("Falha no ciclo de monitoramento (tentativa %d/%d)", attempt, attempts)
                if attempt < attempts:
                    await asyncio.sleep(self.settings.retry_backoff_seconds * attempt)
            finally:
                session.close()

    async def _loop(self) -> None:
        while not self._stop.is_set():
            import datetime as _dt

            await self._run_cycle_with_retry()
            self.last_poll_at = _dt.datetime.now(_dt.timezone.utc)
            try:
                await asyncio.wait_for(self._stop.wait(), timeout=self.settings.poll_interval_seconds)
            except asyncio.TimeoutError:
                pass

    def start(self) -> None:
        if self._task is None:
            self._stop.clear()
            self._task = asyncio.create_task(self._loop())

    async def stop(self) -> None:
        self._stop.set()
        if self._task:
            await self._task
            self._task = None
