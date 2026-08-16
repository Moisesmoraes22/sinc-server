"""Testes do parser da fonte real, usando respostas simuladas (respx),
ja que o ambiente de teste nao tem acesso de rede a fonte real (ver
docs/architecture.md). Cobrem os dois formatos plausiveis: JSON e HTML.
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
                {"id": "srv1", "name": "Servidor Principal", "status": "online"},
                {"id": "srv2", "name": "Servidor ERP", "status": "offline"},
            ],
            headers={"content-type": "application/json"},
        )
    )
    client = RealSourceClient(url=URL)
    readings = await client.fetch_servers()

    assert len(readings) == 2
    by_id = {r.external_id: r for r in readings}
    assert by_id["srv1"].is_up is True
    assert by_id["srv2"].is_up is False


@pytest.mark.asyncio
@respx.mock
async def test_parses_json_wrapped_in_servers_key():
    respx.get(URL).mock(
        return_value=httpx.Response(
            200,
            json={"servers": [{"nome": "Banco de Dados", "situacao": "ativo"}]},
            headers={"content-type": "application/json"},
        )
    )
    client = RealSourceClient(url=URL)
    readings = await client.fetch_servers()

    assert len(readings) == 1
    assert readings[0].name == "Banco de Dados"
    assert readings[0].is_up is True


@pytest.mark.asyncio
@respx.mock
async def test_falls_back_to_html_table_parsing():
    html = """
    <html><body>
    <table>
      <tr><th>Nome</th><th>Status</th></tr>
      <tr><td>Servidor Principal</td><td>ONLINE</td></tr>
      <tr><td>Servidor ERP</td><td>OFFLINE</td></tr>
    </table>
    </body></html>
    """
    respx.get(URL).mock(return_value=httpx.Response(200, text=html, headers={"content-type": "text/html"}))
    client = RealSourceClient(url=URL)
    readings = await client.fetch_servers()

    assert len(readings) == 2
    statuses = {r.name: r.is_up for r in readings}
    assert statuses["Servidor Principal"] is True
    assert statuses["Servidor ERP"] is False


@pytest.mark.asyncio
@respx.mock
async def test_http_error_propagates():
    respx.get(URL).mock(return_value=httpx.Response(500))
    client = RealSourceClient(url=URL)
    with pytest.raises(httpx.HTTPStatusError):
        await client.fetch_servers()
