import uuid
import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from app.services.route_store import save_location, finalize_route, save_pois


@pytest.mark.asyncio
async def test_save_location_single_query():
    db = AsyncMock()
    await save_location(db, "route-uuid", lat=52.23, lng=21.01)

    assert db.execute.await_count == 1
    sql = str(db.execute.call_args.args[0])
    assert "CASE" in sql
    assert "ST_MakePoint" in sql
    db.commit.assert_awaited_once()


@pytest.mark.asyncio
async def test_finalize_route_skips_when_fewer_than_2_points():
    db = AsyncMock()
    result = MagicMock()
    result.scalar.return_value = 1
    db.execute = AsyncMock(return_value=result)

    await finalize_route(db, str(uuid.uuid4()))

    db.commit.assert_not_awaited()


@pytest.mark.asyncio
async def test_finalize_route_saves_distance_and_ended_at():
    db = AsyncMock()
    points_result = MagicMock()
    points_result.scalar.return_value = 3
    dist_result = MagicMock()
    dist_result.scalar.return_value = 1234.5
    db.execute = AsyncMock(side_effect=[points_result, dist_result])
    route_mock = MagicMock()
    db.get = AsyncMock(return_value=route_mock)

    await finalize_route(db, str(uuid.uuid4()))

    assert route_mock.ended_at is not None
    assert route_mock.distance_m == 1234.5
    db.commit.assert_awaited_once()


@pytest.mark.asyncio
async def test_save_pois_adds_all_records():
    poi_list = [
        {"id": "poi-1", "name": "Museum", "lat": 50.0, "lng": 19.0},
        {"id": "poi-2", "name": "Park", "lat": 50.1, "lng": 19.1},
    ]
    route_id = str(uuid.uuid4())

    with patch("app.services.route_store.AsyncSessionLocal") as mock_session_cls:
        mock_db = AsyncMock()
        mock_db.__aenter__ = AsyncMock(return_value=mock_db)
        mock_db.__aexit__ = AsyncMock(return_value=False)
        mock_session_cls.return_value = mock_db

        await save_pois(route_id, poi_list)

    assert mock_db.add.call_count == 2
    mock_db.commit.assert_awaited_once()
