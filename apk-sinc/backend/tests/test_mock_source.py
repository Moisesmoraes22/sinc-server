import pytest

from app.monitor.mock_source import MockSourceClient


@pytest.mark.asyncio
async def test_default_all_up():
    client = MockSourceClient(servers=[{"id": "s1", "name": "Servidor 1"}])
    readings = await client.fetch_servers()
    assert len(readings) == 1
    assert readings[0].is_up is True
    assert readings[0].raw_status == "online"


@pytest.mark.asyncio
async def test_scenario_sequence_consumed_in_order():
    client = MockSourceClient(servers=[{"id": "s1", "name": "Servidor 1"}])
    client.set_scenario("s1", ["up", "down", "down", "down", "up"])

    results = []
    for _ in range(5):
        readings = await client.fetch_servers()
        results.append(readings[0].is_up)

    assert results == [True, False, False, False, True]


@pytest.mark.asyncio
async def test_scenario_falls_back_to_up_after_exhausted():
    client = MockSourceClient(servers=[{"id": "s1", "name": "Servidor 1"}])
    client.set_scenario("s1", ["down"])

    first = (await client.fetch_servers())[0]
    second = (await client.fetch_servers())[0]

    assert first.is_up is False
    assert second.is_up is True


@pytest.mark.asyncio
async def test_clear_scenarios():
    client = MockSourceClient(servers=[{"id": "s1", "name": "Servidor 1"}])
    client.set_scenario("s1", ["down", "down"])
    client.clear_scenarios()
    reading = (await client.fetch_servers())[0]
    assert reading.is_up is True
