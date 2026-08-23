"""Cliente para a fonte real de monitoramento de sincronizacao.

O endpoint (".../monitorsincronizacao/") e uma aplicacao GWT (Google Web
Toolkit) legada: a pagina inicial e HTML vazio e os dados reais so aparecem
depois que o JavaScript da aplicacao roda e faz uma chamada RPC ao servidor
num formato binario proprietario do GWT (nao e JSON nem uma tabela HTML
estatica). Esse protocolo depende da ordem exata dos campos da classe DTO
Java do servidor e nao e documentado nem estavel o suficiente para decodificar
na mao com seguranca.

Por isso este cliente usa Playwright (Chromium headless) para renderizar a
pagina como um navegador de verdade faria, esperar a tabela carregar, e ler
os dados diretamente do DOM ja processado pelo GWT. E mais pesado que uma
chamada HTTP simples (~1-3s por consulta) mas e a forma robusta de extrair
dados de uma pagina que so os expoe via JavaScript client-side.

Usamos a API SINCRONA do Playwright (nao a async_api) rodando dentro de
`asyncio.to_thread`, em vez de manter um browser assincrono vivo no loop do
FastAPI. Motivo: no Windows, o loop de eventos padrao do asyncio
(SelectorEventLoop, que e o que o uvicorn usa) nao sabe criar subprocessos -
e o Playwright async precisa disso para abrir o Chromium, o que gera
`NotImplementedError`. A API sincrona do Playwright gerencia o subprocesso do
Chromium na propria thread onde e chamada, sem depender do event loop do
chamador, entao basta rodar essa thread fora do loop principal (via
`asyncio.to_thread`) para nao bloquear as outras requisicoes do FastAPI
enquanto a pagina carrega.

Tabela renderizada (inspecionada em produção, campos podem variar de ordem
mas nao de nome - o parser abaixo casa por nome de coluna, nao por posicao):

    Cod. A7 | Un. Negocio | Revisoes a Enviar | Ultimo Envio |
    Tempo Desde Ultimo Envio | Ultimo Recebimento | Tempo Desde Ultimo Recebimento

Os numeros de "Revisoes a Enviar" vem no formato brasileiro (ponto como
separador de milhar, ex. "34.955"), tratado em `_parse_int_brl`.
"""

import asyncio
import logging
import sys
from datetime import datetime, timezone
from zoneinfo import ZoneInfo

from playwright.sync_api import sync_playwright

from app.config import get_settings
from app.models.domain import SyncUnitReading

logger = logging.getLogger("sinc.real_source")

# A pagina da A7Pharma exibe horarios no fuso local brasileiro, sem indicar o
# offset. Interpretar esses horarios como UTC faz o backend achar que toda
# unidade esta 3h atrasada e marcar tudo como CRITICO indevidamente.
SOURCE_TIMEZONE = ZoneInfo("America/Sao_Paulo")


def _parse_int_brl(value: str | None) -> int | None:
    if not value:
        return None
    cleaned = value.strip().replace(".", "").replace(",", "")
    if not cleaned or not cleaned.lstrip("-").isdigit():
        return None
    return int(cleaned)


def _parse_datetime_brl(value: str | None) -> datetime | None:
    if not value:
        return None
    text = value.strip()
    if not text:
        return None
    for fmt in ("%d/%m/%Y %H:%M:%S", "%d/%m/%Y %H:%M"):
        try:
            parsed = datetime.strptime(text, fmt)
            return parsed.replace(tzinfo=SOURCE_TIMEZONE).astimezone(timezone.utc)
        except ValueError:
            continue
    logger.debug("Nao foi possivel parsear data/hora da fonte real: %r", value)
    return None


_EXTRACT_TABLE_JS = """() => {
    const tables = Array.from(document.querySelectorAll('table'));
    for (const table of tables) {
        const rows = Array.from(table.rows).map(
            tr => Array.from(tr.cells).map(cell => cell.innerText.trim())
        );
        const header = rows[0];
        // A tabela de layout externa tambem "contem" o texto do cabecalho
        // (tudo esta aninhado nela), mas como uma unica celula gigante com
        // o texto da pagina inteira. A tabela de dados real tem poucas
        // colunas curtas - e esse o criterio que as distingue.
        const looksLikeHeaderRow = header
            && header.length >= 4 && header.length <= 12
            && header.every(c => c.length < 60)
            && header.some(c => c.toLowerCase().includes('a7'));
        if (looksLikeHeaderRow) {
            return rows;
        }
    }
    return [];
}"""


class RealSourceClient:
    def __init__(self, url: str | None = None, timeout: float | None = None):
        settings = get_settings()
        self.url = url or settings.source_url
        self.timeout_ms = int((timeout or settings.source_timeout_seconds) * 1000)

    async def close(self) -> None:
        """Nao ha estado persistente para liberar - cada fetch_units() abre e
        fecha seu proprio Chromium na thread onde roda. Mantido por
        compatibilidade com o shutdown hook em app/main.py."""
        return None

    async def fetch_units(self) -> list[SyncUnitReading]:
        rows = await asyncio.to_thread(self._fetch_rows_sync)
        return self._parse_rows(rows)

    def _fetch_rows_sync(self) -> list[list[str]]:
        # O uvicorn forca SelectorEventLoopPolicy no processo inteiro no
        # Windows (necessario para o proprio servidor HTTP), mas essa policy
        # e global e o Playwright cria seu proprio event loop interno nesta
        # thread separada. SelectorEventLoop nao sabe criar subprocessos
        # (create_subprocess_exec -> NotImplementedError), entao reforcamos
        # ProactorEventLoopPolicy aqui, escopado a esta thread de worker -
        # ela nao tem loop rodando ainda e nao interfere no loop principal
        # do FastAPI/uvicorn na thread do servidor.
        if sys.platform == "win32":
            asyncio.set_event_loop_policy(asyncio.WindowsProactorEventLoopPolicy())

        with sync_playwright() as playwright:
            browser = playwright.chromium.launch(headless=True)
            try:
                page = browser.new_page()
                page.goto(self.url, timeout=self.timeout_ms, wait_until="networkidle")
                page.wait_for_selector("table tr", timeout=self.timeout_ms)
                # A pagina GWT aninha tabelas (uma de layout, outra de dados) -
                # ver _EXTRACT_TABLE_JS para como distinguimos a certa.
                return page.evaluate(_EXTRACT_TABLE_JS)
            finally:
                browser.close()

    def _parse_rows(self, rows: list[list[str]]) -> list[SyncUnitReading]:
        if not rows:
            logger.warning("Fonte real nao retornou nenhuma linha de tabela apos renderizar a pagina.")
            return []

        header = [cell.strip().lower() for cell in rows[0]]

        def find_column(*, must_have: list[str], must_not_have: list[str] = []) -> int | None:
            for idx, cell in enumerate(header):
                if all(token in cell for token in must_have) and not any(token in cell for token in must_not_have):
                    return idx
            return None

        # Nota: usar `is None`, nao `or`, para escolher entre a tentativa
        # principal e o fallback - o indice 0 e um valor valido mas "falsy"
        # em Python, e `0 or fallback` avaliaria incorretamente o fallback.
        code_idx = find_column(must_have=["a7"])
        if code_idx is None:
            code_idx = find_column(must_have=["cod"])

        unit_idx = find_column(must_have=["negóc"])
        if unit_idx is None:
            unit_idx = find_column(must_have=["negoc"])

        revisions_idx = find_column(must_have=["revis"])
        last_send_idx = find_column(must_have=["envio"], must_not_have=["tempo", "desde"])
        last_receive_idx = find_column(must_have=["recebimento"], must_not_have=["tempo", "desde"])

        if code_idx is None:
            logger.warning("Nao foi possivel identificar a coluna de codigo A7 no header: %r", header)
            return []

        now = datetime.now(timezone.utc)
        readings: list[SyncUnitReading] = []
        for row in rows[1:]:
            if code_idx >= len(row) or not row[code_idx]:
                continue
            # A tabela usa uma linha de placeholder com uma unica celula em
            # colspan ("Nao existem dados para serem exibidos") quando esta
            # vazia; sem esse filtro ela viraria uma "unidade" fantasma.
            if len(row) < 3:
                continue
            a7_code = row[code_idx]
            business_unit = row[unit_idx] if unit_idx is not None and unit_idx < len(row) else a7_code
            revisions = row[revisions_idx] if revisions_idx is not None and revisions_idx < len(row) else None
            last_send = row[last_send_idx] if last_send_idx is not None and last_send_idx < len(row) else None
            last_receive = row[last_receive_idx] if last_receive_idx is not None and last_receive_idx < len(row) else None

            readings.append(
                SyncUnitReading(
                    a7_code=a7_code,
                    business_unit=business_unit,
                    revisions_to_send=_parse_int_brl(revisions),
                    last_send_at=_parse_datetime_brl(last_send),
                    last_receive_at=_parse_datetime_brl(last_receive),
                    checked_at=now,
                )
            )
        return readings
