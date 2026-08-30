import asyncio
import logging

from app.services.monitor_service import MonitorService

logger = logging.getLogger("sinc.poller")


class Poller:
    """Loop assincrono que roda o ciclo de checagem periodicamente, com retry.

    O intervalo entre checagens e lido de `monitor_settings.check_interval_seconds`
    (via repository) a cada ciclo, para que a mudanca de configuracao valha
    sem precisar reiniciar o backend.
    """

    def __init__(self, monitor_service: MonitorService, retry_attempts: int, retry_backoff_seconds: float):
        self.monitor_service = monitor_service
        self.retry_attempts = retry_attempts
        self.retry_backoff_seconds = retry_backoff_seconds
        self._task: asyncio.Task | None = None
        self._stop = asyncio.Event()
        self.last_poll_at = None
        # last_poll_at sozinho nao distingue "o ciclo rodou" de "o ciclo
        # realmente trouxe dados" - fetch_units() pode falhar (ex.:
        # Playwright travando apos rodar por horas) e run_check_cycle()
        # absorve isso devolvendo lista vazia, entao o ciclo "roda" sem
        # nunca atualizar nenhuma unidade. Esses dois campos existem pra
        # essa diferenca aparecer no /api/health sem precisar de log ao vivo.
        self.last_successful_fetch_at = None
        self.consecutive_fetch_failures = 0

    async def _run_cycle_with_retry(self) -> None:
        attempts = max(1, self.retry_attempts)
        for attempt in range(1, attempts + 1):
            try:
                readings = await self.monitor_service.run_check_cycle()
                if readings:
                    import datetime as _dt

                    self.last_successful_fetch_at = _dt.datetime.now(_dt.timezone.utc)
                    self.consecutive_fetch_failures = 0
                else:
                    self.consecutive_fetch_failures += 1
                return
            except Exception:
                logger.exception("Falha no ciclo de monitoramento (tentativa %d/%d)", attempt, attempts)
                if attempt < attempts:
                    await asyncio.sleep(self.retry_backoff_seconds * attempt)
        self.consecutive_fetch_failures += 1

    async def _loop(self) -> None:
        while not self._stop.is_set():
            import datetime as _dt

            await self._run_cycle_with_retry()
            self.last_poll_at = _dt.datetime.now(_dt.timezone.utc)

            try:
                interval = self.monitor_service.repository.get_settings().check_interval_seconds
            except Exception:
                logger.exception("Falha ao ler check_interval_seconds; usando fallback de 60s")
                interval = 60

            try:
                await asyncio.wait_for(self._stop.wait(), timeout=interval)
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
