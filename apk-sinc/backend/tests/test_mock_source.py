import pytest

from app.models.domain import ATENCAO, CRITICO, NORMAL
from app.monitor.mock_source import MockSourceClient


@pytest.mark.asyncio
async def test_default_all_normal():
    client = MockSourceClient(units=[{"a7_code": "A7-1", "business_unit": "Unidade 1"}])
    readings = await client.fetch_units()
    assert len(readings) == 1
    reading = readings[0]
    assert reading.a7_code == "A7-1"
    assert reading.last_send_at is not None
    assert reading.last_receive_at is not None


@pytest.mark.asyncio
async def test_scenario_sequence_consumed_in_order():
    client = MockSourceClient(units=[{"a7_code": "A7-1", "business_unit": "Unidade 1"}])
    client.set_scenario("A7-1", [NORMAL, ATENCAO, CRITICO, NORMAL])

    elapsed_by_tick = []
    for _ in range(4):
        readings = await client.fetch_units()
        reading = readings[0]
        elapsed_minutes = (reading.checked_at - reading.last_send_at).total_seconds() / 60
        elapsed_by_tick.append(round(elapsed_minutes))

    # NORMAL < 5 < ATENCAO < 15 < CRITICO
    assert elapsed_by_tick[0] < 5
    assert 5 <= elapsed_by_tick[1] < 15
    assert elapsed_by_tick[2] >= 15
    assert elapsed_by_tick[3] < 5


@pytest.mark.asyncio
async def test_scenario_falls_back_to_normal_after_exhausted():
    client = MockSourceClient(units=[{"a7_code": "A7-1", "business_unit": "Unidade 1"}])
    client.set_scenario("A7-1", [CRITICO])

    first = (await client.fetch_units())[0]
    second = (await client.fetch_units())[0]

    first_elapsed = (first.checked_at - first.last_send_at).total_seconds() / 60
    second_elapsed = (second.checked_at - second.last_send_at).total_seconds() / 60

    assert first_elapsed >= 15
    assert second_elapsed < 5


@pytest.mark.asyncio
async def test_clear_scenarios():
    client = MockSourceClient(units=[{"a7_code": "A7-1", "business_unit": "Unidade 1"}])
    client.set_scenario("A7-1", [CRITICO, CRITICO])
    client.clear_scenarios()
    reading = (await client.fetch_units())[0]
    elapsed = (reading.checked_at - reading.last_send_at).total_seconds() / 60
    assert elapsed < 5


@pytest.mark.asyncio
async def test_multiple_default_units():
    client = MockSourceClient()
    readings = await client.fetch_units()
    assert len(readings) == 4
    codes = {r.a7_code for r in readings}
    assert "A7-0001" in codes
