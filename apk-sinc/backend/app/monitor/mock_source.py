"""Fonte simulada (MOCK), usada quando SOURCE_MODE=mock.

Permite desenvolvimento e testes sem depender da infraestrutura real.
Suporta cenarios controlados via `set_scenario`, usados nos testes de
anti-spam, tres falhas consecutivas e recuperacao.
"""

from datetime import datetime, timezone

from app.models.schemas import ServerReading

DEFAULT_SERVERS = [
    {"id": "server-principal", "name": "Servidor Principal"},
    {"id": "server-erp", "name": "Servidor ERP"},
    {"id": "server-banco-dados", "name": "Banco de Dados"},
    {"id": "server-api-vendas", "name": "API de Vendas"},
]


class MockSourceClient:
    """Cliente simulado. Cada chamada a `fetch_servers` avanca um "tick".

    Por padrao todos os servidores respondem "up". E possivel forcar uma
    sequencia de resultados ("up"/"down") por servidor via `set_scenario`,
    que e consumida em ordem a cada tick subsequente; ao esgotar a
    sequencia, o servidor volta ao padrao "up".
    """

    def __init__(self, servers: list[dict] | None = None):
        self.servers = servers if servers is not None else DEFAULT_SERVERS
        self._scenarios: dict[str, list[bool]] = {}

    def set_scenario(self, server_id: str, sequence: list[str]) -> None:
        self._scenarios[server_id] = [s.lower() == "up" for s in sequence]

    def clear_scenarios(self) -> None:
        self._scenarios = {}

    def _next_status(self, server_id: str) -> bool:
        queue = self._scenarios.get(server_id)
        if queue:
            return queue.pop(0)
        return True

    async def fetch_servers(self) -> list[ServerReading]:
        now = datetime.now(timezone.utc)
        readings = []
        for server in self.servers:
            is_up = self._next_status(server["id"])
            readings.append(
                ServerReading(
                    external_id=server["id"],
                    name=server["name"],
                    raw_status="online" if is_up else "offline",
                    is_up=is_up,
                    response_time_ms=120 if is_up else None,
                    checked_at=now,
                )
            )
        return readings
