"""Cliente para a fonte real de monitoramento de sincronizacao.

IMPORTANTE: este cliente NAO PUDE ser validado contra a URL real durante o
desenvolvimento porque o ambiente de build nao tem rota de rede ate
`cli-1237.ddns.a7cloud.net.br:8080` (ver docs/architecture.md, secao 1).

O endpoint (".../monitorsincronizacao/") sugere fortemente um monitor de
sincronizacao por unidade de negocio (codigo A7), com timestamps de ultimo
envio/recebimento - e esse e o modelo de dominio adotado
(app/models/domain.py: SyncUnitReading). O parser abaixo e tolerante a
nomes de campo em portugues/ingles e a dois formatos plausiveis:

1. JSON estruturado (lista de unidades) - tentado primeiro.
2. HTML com uma tabela - fallback via BeautifulSoup, tentando casar as
   colunas do cabecalho com os campos esperados.

Assim que o endpoint puder ser inspecionado de um ambiente com acesso
(rode `python backend/scripts/inspect_source.py`), ajuste `_parse_json`
e/ou `_parse_html` para o formato exato encontrado, mantendo a mesma
assinatura publica `fetch_units()`.
"""

import logging
import re
from datetime import datetime, timezone

import httpx
from bs4 import BeautifulSoup

from app.config import get_settings
from app.models.domain import SyncUnitReading

logger = logging.getLogger("sinc.real_source")

A7_CODE_KEYS = ["a7_code", "codigo_a7", "codigoa7", "a7code", "codigo", "id"]
BUSINESS_UNIT_KEYS = ["business_unit", "unidade", "unidade_negocio", "nome", "empresa", "filial"]
REVISIONS_KEYS = ["revisions_to_send", "revisoes_a_enviar", "revisoes", "pendentes", "pending", "revisions"]
LAST_SEND_KEYS = ["last_send_at", "ultimo_envio", "data_envio", "envio", "send_at", "last_send"]
LAST_RECEIVE_KEYS = ["last_receive_at", "ultimo_recebimento", "data_recebimento", "recebimento", "receive_at", "last_receive"]


def _get_first(item: dict, keys: list[str]):
    lower_item = {str(k).strip().lower(): v for k, v in item.items()}
    for key in keys:
        if key in lower_item and lower_item[key] not in (None, ""):
            return lower_item[key]
    return None


def _parse_datetime(value) -> datetime | None:
    if value is None:
        return None
    if isinstance(value, (int, float)):
        try:
            return datetime.fromtimestamp(value, tz=timezone.utc)
        except (ValueError, OSError):
            return None
    text = str(value).strip()
    if not text:
        return None
    for fmt in ("%Y-%m-%dT%H:%M:%S%z", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%d %H:%M:%S", "%d/%m/%Y %H:%M:%S", "%d/%m/%Y %H:%M"):
        try:
            parsed = datetime.strptime(text, fmt)
            return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)
        except ValueError:
            continue
    try:
        parsed = datetime.fromisoformat(text.replace("Z", "+00:00"))
        return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)
    except ValueError:
        logger.debug("Nao foi possivel parsear data/hora: %r", value)
        return None


def _slugify(name: str) -> str:
    slug = re.sub(r"[^A-Za-z0-9]+", "-", name.strip().upper())
    return slug.strip("-") or "UNIDADE"


class RealSourceClient:
    def __init__(self, url: str | None = None, timeout: float | None = None):
        settings = get_settings()
        self.url = url or settings.source_url
        self.timeout = timeout or settings.source_timeout_seconds

    async def fetch_units(self) -> list[SyncUnitReading]:
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.get(self.url)
            response.raise_for_status()

        try:
            return self._parse_json(response.json())
        except ValueError:
            pass

        return self._parse_html(response.text)

    def _parse_json(self, payload) -> list[SyncUnitReading]:
        now = datetime.now(timezone.utc)
        items = payload if isinstance(payload, list) else payload.get("units") or payload.get("data") or payload.get("servers") or []
        readings: list[SyncUnitReading] = []
        for item in items:
            business_unit = str(_get_first(item, BUSINESS_UNIT_KEYS) or "Unidade desconhecida")
            a7_code = str(_get_first(item, A7_CODE_KEYS) or _slugify(business_unit))
            revisions = _get_first(item, REVISIONS_KEYS)
            readings.append(
                SyncUnitReading(
                    a7_code=a7_code,
                    business_unit=business_unit,
                    revisions_to_send=int(revisions) if revisions is not None else None,
                    last_send_at=_parse_datetime(_get_first(item, LAST_SEND_KEYS)),
                    last_receive_at=_parse_datetime(_get_first(item, LAST_RECEIVE_KEYS)),
                    checked_at=now,
                    raw=item if isinstance(item, dict) else {},
                )
            )
        return readings

    def _parse_html(self, html: str) -> list[SyncUnitReading]:
        now = datetime.now(timezone.utc)
        soup = BeautifulSoup(html, "html.parser")
        readings: list[SyncUnitReading] = []

        table = soup.find("table")
        if table is None:
            logger.warning(
                "Nao foi possivel identificar uma tabela de unidades no HTML da fonte real; "
                "ajuste RealSourceClient._parse_html apos inspecionar o endpoint real."
            )
            return readings

        rows = table.find_all("tr")
        if not rows:
            return readings

        header_cells = [c.get_text(strip=True).lower() for c in rows[0].find_all(["th", "td"])]

        def _col_index(keys: list[str]) -> int | None:
            for idx, header in enumerate(header_cells):
                if any(key in header for key in keys):
                    return idx
            return None

        code_idx = _col_index(A7_CODE_KEYS) or 0
        unit_idx = _col_index(BUSINESS_UNIT_KEYS)
        send_idx = _col_index(LAST_SEND_KEYS)
        receive_idx = _col_index(LAST_RECEIVE_KEYS)
        revisions_idx = _col_index(REVISIONS_KEYS)

        for row in rows[1:]:
            cells = [c.get_text(strip=True) for c in row.find_all(["td", "th"])]
            if not cells or len(cells) <= code_idx:
                continue
            a7_code = cells[code_idx]
            if not a7_code:
                continue
            business_unit = cells[unit_idx] if unit_idx is not None and unit_idx < len(cells) else a7_code
            revisions = cells[revisions_idx] if revisions_idx is not None and revisions_idx < len(cells) else None
            last_send = cells[send_idx] if send_idx is not None and send_idx < len(cells) else None
            last_receive = cells[receive_idx] if receive_idx is not None and receive_idx < len(cells) else None

            readings.append(
                SyncUnitReading(
                    a7_code=a7_code,
                    business_unit=business_unit,
                    revisions_to_send=int(revisions) if revisions and revisions.isdigit() else None,
                    last_send_at=_parse_datetime(last_send),
                    last_receive_at=_parse_datetime(last_receive),
                    checked_at=now,
                )
            )
        return readings
