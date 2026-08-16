"""Cliente para a fonte real de monitoramento.

IMPORTANTE: este cliente NAO PUDE ser validado contra a URL real durante o
desenvolvimento porque o ambiente de build nao tem rota de rede ate
`cli-1237.ddns.a7cloud.net.br:8080` (ver docs/architecture.md, secao 1).

O parser abaixo foi escrito para ser tolerante a dois formatos plausiveis
para um endpoint chamado ".../monitorsincronizacao/":

1. JSON estruturado (lista de servidores com nome/status) - tentado primeiro.
2. HTML com uma tabela ou lista de linhas "nome + status" - fallback via
   BeautifulSoup, procurando por palavras-chave ONLINE/OFFLINE/ATIVO/INATIVO
   no texto de cada linha candidata.

Assim que o endpoint puder ser inspecionado de um ambiente com acesso
(rode `python backend/scripts/inspect_source.py`), ajuste `_parse_json` e/ou
`_parse_html` para o formato exato encontrado, mantendo a mesma assinatura
publica `fetch_servers()`.
"""

import logging
import re
from datetime import datetime, timezone

import httpx
from bs4 import BeautifulSoup

from app.config import get_settings
from app.models.schemas import ServerReading

logger = logging.getLogger("sinc.real_source")

UP_KEYWORDS = {"online", "ok", "ativo", "up", "1", "true", "conectado"}
DOWN_KEYWORDS = {"offline", "erro", "inativo", "down", "0", "false", "desconectado"}


def _slugify(name: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "-", name.strip().lower())
    return slug.strip("-") or "server"


def _status_to_bool(raw_status: str) -> bool:
    normalized = raw_status.strip().lower()
    if normalized in UP_KEYWORDS:
        return True
    if normalized in DOWN_KEYWORDS:
        return False
    # fallback heuristico: presenca da palavra "off" indica indisponibilidade
    return "off" not in normalized and "erro" not in normalized


class RealSourceClient:
    def __init__(self, url: str | None = None, timeout: float | None = None):
        settings = get_settings()
        self.url = url or settings.source_url
        self.timeout = timeout or settings.source_timeout_seconds

    async def fetch_servers(self) -> list[ServerReading]:
        started = datetime.now(timezone.utc)
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.get(self.url)
            response.raise_for_status()
        elapsed_ms = int((datetime.now(timezone.utc) - started).total_seconds() * 1000)
        content_type = response.headers.get("content-type", "")

        if "application/json" in content_type:
            return self._parse_json(response.json(), elapsed_ms)

        try:
            return self._parse_json(response.json(), elapsed_ms)
        except ValueError:
            pass

        return self._parse_html(response.text, elapsed_ms)

    def _parse_json(self, payload, elapsed_ms: int) -> list[ServerReading]:
        now = datetime.now(timezone.utc)
        items = payload if isinstance(payload, list) else payload.get("servers") or payload.get("data") or []
        readings: list[ServerReading] = []
        for item in items:
            name = str(item.get("name") or item.get("nome") or item.get("servidor") or "desconhecido")
            raw_status = str(item.get("status") or item.get("situacao") or item.get("state") or "")
            external_id = str(item.get("id") or _slugify(name))
            readings.append(
                ServerReading(
                    external_id=external_id,
                    name=name,
                    raw_status=raw_status,
                    is_up=_status_to_bool(raw_status),
                    response_time_ms=item.get("response_time_ms") or elapsed_ms,
                    checked_at=now,
                )
            )
        return readings

    def _parse_html(self, html: str, elapsed_ms: int) -> list[ServerReading]:
        now = datetime.now(timezone.utc)
        soup = BeautifulSoup(html, "html.parser")
        readings: list[ServerReading] = []

        rows = soup.select("table tr")
        if rows:
            for row in rows:
                cells = [c.get_text(strip=True) for c in row.find_all(["td", "th"])]
                if len(cells) < 2:
                    continue
                name, raw_status = cells[0], cells[1]
                if not name or name.lower() in {"nome", "servidor", "name"}:
                    continue
                readings.append(
                    ServerReading(
                        external_id=_slugify(name),
                        name=name,
                        raw_status=raw_status,
                        is_up=_status_to_bool(raw_status),
                        response_time_ms=elapsed_ms,
                        checked_at=now,
                    )
                )
            if readings:
                return readings

        logger.warning(
            "Nao foi possivel identificar uma tabela de servidores no HTML da fonte real; "
            "ajuste RealSourceClient._parse_html apos inspecionar o endpoint real."
        )
        return readings
