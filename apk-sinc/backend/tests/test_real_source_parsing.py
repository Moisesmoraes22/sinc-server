"""Testes do parser da fonte real, usando respostas simuladas (respx),
ja que o ambiente de teste nao tem acesso de rede a fonte real (ver
docs/architecture.md).
"""

import httpx
import pytest
import respx

from app.monitor.real_source import RealSourceClient

URL = "http://cli-1237.ddns.a7cloud.net.br:8080/online/monitorsincronizacao/"


@pytest.mark.asyncio
@respx.mock
async def test_parses_json_list_format():
    respx.get(URL).mock(
        return_value=httpx.Response(
            200,
            json=[
                {
                    "a7_code": "A7-0001",
                    "business_unit": "Unidade Matriz",
                    "revisions_to_send": 3,
                    "last_send_at": "2026-01-01T10:00:00Z",
                    "last_receive_at": "2026-01-01T10:05:00Z",
                },
            ],
            headers={"content-type": "application/json"},
        )
    )
    client = RealSourceClient(url=URL)
    readings = await client.fetch_units()

    assert len(readings) == 1
    reading = readings[0]
    assert reading.a7_code == "A7-0001"
    assert reading.business_unit == "Unidade Matriz"
    assert reading.revisions_to_send == 3
    assert reading.last_send_at is not None
    assert reading.last_receive_at is not None


@pytest.mark.asyncio
@respx.mock
async def test_parses_json_with_portuguese_field_names():
    respx.get(URL).mock(
        return_value=httpx.Response(
            200,
            json={
                "data": [
                    {
                        "codigo_a7": "A7-0002",
                        "unidade": "Filial Sul",
                        "revisoes_a_enviar": 7,
                        "ultimo_envio": "2026-01-01T09:00:00",
                        "ultimo_recebimento": "2026-01-01T09:10:00",
                    }
                ]
            },
            headers={"content-type": "application/json"},
        )
    )
    client = RealSourceClient(url=URL)
    readings = await client.fetch_units()

    assert len(readings) == 1
    reading = readings[0]
    assert reading.a7_code == "A7-0002"
    assert reading.business_unit == "Filial Sul"
    assert reading.revisions_to_send == 7


@pytest.mark.asyncio
@respx.mock
async def test_falls_back_to_html_table_parsing():
    html = """
    <html><body>
    <table>
      <tr><th>Codigo A7</th><th>Unidade</th><th>Ultimo Envio</th><th>Ultimo Recebimento</th></tr>
      <tr><td>A7-0003</td><td>Filial Norte</td><td>01/01/2026 08:00</td><td>01/01/2026 08:05</td></tr>
    </table>
    </body></html>
    """
    respx.get(URL).mock(return_value=httpx.Response(200, text=html, headers={"content-type": "text/html"}))
    client = RealSourceClient(url=URL)
    readings = await client.fetch_units()

    assert len(readings) == 1
    reading = readings[0]
    assert reading.a7_code == "A7-0003"
    assert reading.business_unit == "Filial Norte"
    assert reading.last_send_at is not None


@pytest.mark.asyncio
@respx.mock
async def test_http_error_propagates():
    respx.get(URL).mock(return_value=httpx.Response(500))
    client = RealSourceClient(url=URL)
    with pytest.raises(httpx.HTTPStatusError):
        await client.fetch_units()
