import logging
from datetime import datetime, timezone

from sqlalchemy.orm import Session

from app.database.repository import EventRepository, ServerRepository
from app.models.orm import EventORM, ServerORM
from app.models.schemas import ServerReading
from app.monitor.source_client import SourceClient
from app.notifications.fcm_client import FcmClient
from app.services.state_machine import ServerState, evaluate

logger = logging.getLogger("sinc.monitor_service")


class MonitorService:
    def __init__(self, source_client: SourceClient, fcm_client: FcmClient | None = None):
        self.source_client = source_client
        self.fcm_client = fcm_client or FcmClient()

    async def run_check_cycle(self, session: Session) -> list[ServerReading]:
        """Busca a fonte, aplica a maquina de estados por servidor, persiste e notifica.

        Retorna as leituras obtidas (util em testes). Erros de rede ao buscar a
        fonte inteira sao logados e absorvidos, sem interromper o processo.
        """
        try:
            readings = await self.source_client.fetch_servers()
        except Exception:
            logger.exception("Falha ao consultar a fonte de monitoramento")
            return []

        server_repo = ServerRepository(session)
        event_repo = EventRepository(session)

        for reading in readings:
            self._process_reading(reading, server_repo, event_repo)

        server_repo.commit()
        event_repo.commit()
        return readings

    def _process_reading(
        self, reading: ServerReading, server_repo: ServerRepository, event_repo: EventRepository
    ) -> None:
        existing = server_repo.get(reading.external_id)
        current_state = ServerState(
            status=existing.status if existing else "ONLINE",
            consecutive_failures=existing.consecutive_failures if existing else 0,
        )

        transition = evaluate(current_state, reading.is_up)
        now = datetime.now(timezone.utc)

        server = ServerORM(
            id=reading.external_id,
            name=reading.name,
            status=transition.new_status,
            consecutive_failures=transition.consecutive_failures,
            response_time_ms=reading.response_time_ms,
            last_check=now,
            last_down_at=now if transition.new_status == "OFFLINE" and transition.previous_status != "OFFLINE" else None,
            last_up_at=now if transition.new_status == "ONLINE" and transition.previous_status == "OFFLINE" else None,
            down_count=(existing.down_count if existing else 0)
            + (1 if transition.new_status == "OFFLINE" and transition.previous_status != "OFFLINE" else 0),
        )
        server_repo.upsert(server)

        if transition.previous_status != transition.new_status:
            event = EventORM(
                server_id=reading.external_id,
                server_name=reading.name,
                from_status=transition.previous_status,
                to_status=transition.new_status,
                occurred_at=now,
                reason=transition.reason,
                response_time_ms=reading.response_time_ms,
            )
            event_repo.add(event)
            logger.info(
                "Servidor %s: %s -> %s (falhas=%d)",
                reading.name,
                transition.previous_status,
                transition.new_status,
                transition.consecutive_failures,
            )

        if transition.should_notify:
            self.fcm_client.send_status_change(
                reading.external_id, reading.name, transition.new_status, transition.reason
            )
