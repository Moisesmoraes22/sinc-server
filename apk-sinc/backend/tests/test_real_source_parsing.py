"""Testes do parser da fonte real. A fonte e uma pagina GWT renderizada via
Playwright (Chromium) - nao ha como mockar isso com respx/httpx, entao os
testes exercitam diretamente a logica pura de parsing (`_parse_rows` e os
helpers `_parse_int_brl`/`_parse_datetime_brl`), a partir de linhas de tabela
como o `page.evaluate(_EXTRACT_TABLE_JS)` retornaria. `fetch_units()` (que
depende de um browser real) e validado em producao, nao aqui.
"""

from datetime import timezone

from app.monitor.real_source import RealSourceClient, _parse_datetime_brl, _parse_int_brl

HEADER = ["Cod. A7", "Un. Negocio", "Revisoes a Enviar", "Ultimo Envio", "Tempo Desde Ultimo Envio", "Ultimo Recebimento", "Tempo Desde Ultimo Recebimento"]


def _client() -> RealSourceClient:
    return RealSourceClient(url="http://fake-source.test/")


def test_parse_int_brl_handles_thousands_separator():
    assert _parse_int_brl("34.955") == 34955
    assert _parse_int_brl("3") == 3
    assert _parse_int_brl(None) is None
    assert _parse_int_brl("") is None
    assert _parse_int_brl("abc") is None


def test_parse_datetime_brl_converts_from_sao_paulo_to_utc():
    parsed = _parse_datetime_brl("01/01/2026 10:00:00")
    assert parsed is not None
    assert parsed.tzinfo == timezone.utc
    # Sao Paulo (UTC-3, sem horario de verao em 2026) -> 13:00 UTC.
    assert parsed.hour == 13

    assert _parse_datetime_brl(None) is None
    assert _parse_datetime_brl("nao e uma data") is None


def test_parse_rows_extracts_readings_by_column_name():
    rows = [
        HEADER,
        ["A7-0001", "Unidade Matriz", "34.955", "01/01/2026 10:00:00", "5 min", "01/01/2026 10:05:00", "2 min"],
    ]
    readings = _client()._parse_rows(rows)

    assert len(readings) == 1
    reading = readings[0]
    assert reading.a7_code == "A7-0001"
    assert reading.business_unit == "Unidade Matriz"
    assert reading.revisions_to_send == 34955
    assert reading.last_send_at is not None
    assert reading.last_receive_at is not None


def test_parse_rows_ignores_placeholder_and_incomplete_rows():
    rows = [HEADER, ["Nao existem dados para serem exibidos"]]
    assert _client()._parse_rows(rows) == []


def test_parse_rows_returns_empty_when_no_rows():
    assert _client()._parse_rows([]) == []


def test_parse_rows_returns_empty_when_a7_column_not_found():
    rows = [["Foo", "Bar"], ["1", "2"]]
    assert _client()._parse_rows(rows) == []
