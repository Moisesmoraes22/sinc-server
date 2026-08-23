"""Fonte simulada (MOCK) do monitor de sincronizacao.

Usada quando SOURCE_MODE=mock, para desenvolver e testar sem depender da
infraestrutura real. Os dados simulados sao persistidos normalmente no
repository configurado (Supabase ou SQLite) - o MOCK so troca a origem dos
dados, nao o destino.

Cada chamada a `fetch_units` representa um "tick" (uma checagem). E
possivel forcar uma sequencia de status por unidade via `set_scenario`,
usando os proprios rotulos de status (NORMAL/ATENCAO/CRITICO) - o mock
traduz cada rotulo em um tempo decorrido desde o ultimo envio/recebimento
compativel com os thresholds padrao (5 / 15 minutos), o que e suficiente
para exercitar a maquina de estados real (services/state_machine.py) de
ponta a ponta.
"""

from datetime import datetime, timedelta, timezone

from app.models.domain import ATENCAO, CRITICO, NORMAL, SyncUnitReading

DEFAULT_UNITS = [
    {"a7_code": "A7-0001", "business_unit": "Unidade Matriz"},
    {"a7_code": "A7-0002", "business_unit": "Unidade Filial Sul"},
    {"a7_code": "A7-0003", "business_unit": "Unidade Filial Norte"},
    {"a7_code": "A7-0004", "business_unit": "Unidade Filial Leste"},
]

# Minutos decorridos usados para cada rotulo de cenario, coerentes com os
# thresholds padrao (warning=5, critical=15) semeados em
# supabase/migrations/004_seed_settings.sql.
_ELAPSED_MINUTES_FOR_LABEL = {
    NORMAL: 1,
    ATENCAO: 8,
    CRITICO: 25,
}


class MockSourceClient:
    def __init__(self, units: list[dict] | None = None):
        self.units = units if units is not None else DEFAULT_UNITS
        self._scenarios: dict[str, list[str]] = {}

    def set_scenario(self, a7_code: str, sequence: list[str]) -> None:
        """sequence: lista de "NORMAL" | "ATENCAO" | "CRITICO" (case-insensitive),
        consumida em ordem a cada tick. Ao esgotar, volta ao padrao NORMAL."""
        self._scenarios[a7_code] = [s.upper() for s in sequence]

    def clear_scenarios(self) -> None:
        self._scenarios = {}

    def _next_label(self, a7_code: str) -> str:
        queue = self._scenarios.get(a7_code)
        if queue:
            return queue.pop(0)
        return NORMAL

    async def fetch_units(self) -> list[SyncUnitReading]:
        now = datetime.now(timezone.utc)
        readings = []
        for unit in self.units:
            label = self._next_label(unit["a7_code"])
            elapsed = _ELAPSED_MINUTES_FOR_LABEL.get(label, _ELAPSED_MINUTES_FOR_LABEL[NORMAL])
            reference = now - timedelta(minutes=elapsed)
            readings.append(
                SyncUnitReading(
                    a7_code=unit["a7_code"],
                    business_unit=unit["business_unit"],
                    revisions_to_send=0 if label == NORMAL else 5,
                    last_send_at=reference,
                    last_receive_at=reference,
                    checked_at=now,
                )
            )
        return readings
