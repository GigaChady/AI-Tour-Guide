import json
import uuid
import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from starlette.testclient import TestClient
from starlette.websockets import WebSocketDisconnect

from app.main import app
from app.core.redis import get_redis
from app.core.database import get_db


def make_mock_db():
    db = AsyncMock()
    route_obj = MagicMock()
    route_obj.id = uuid.uuid4()
    db.add = MagicMock()
    db.refresh = AsyncMock()
    db.get = AsyncMock(return_value=route_obj)
    scalar_none = MagicMock()
    scalar_none.scalar_one_or_none.return_value = None
    scalar_n = MagicMock()
    scalar_n.scalar.return_value = None
    db.execute = AsyncMock(side_effect=[scalar_none, scalar_n, scalar_n])
    return db


def make_mock_redis():
    async def fake_listen():
        yield {"type": "subscribe", "data": 1}

    pubsub = AsyncMock()
    pubsub.listen = fake_listen
    redis = MagicMock()
    redis.pubsub.return_value = pubsub
    redis.set = AsyncMock()
    redis.publish = AsyncMock()
    redis.get = AsyncMock(return_value=None)
    redis.delete = AsyncMock()
    return redis


def test_ws_endpoint_rejects_bad_token():
    mock_db = make_mock_db()
    mock_redis = make_mock_redis()

    async def override_db():
        yield mock_db

    got_response = False
    with patch("app.services.tour_stream.token_service.verify_access_token", side_effect=ValueError("bad")):
        app.dependency_overrides[get_db] = override_db
        app.dependency_overrides[get_redis] = lambda: mock_redis
        try:
            with TestClient(app) as client:
                try:
                    with client.websocket_connect("/route/ws") as ws:
                        ws.send_json({"token": "bad-token"})
                        ws.receive_json()
                        got_response = True
                except Exception:
                    pass  # expected — server closed with code 4401
        finally:
            app.dependency_overrides.pop(get_db, None)
            app.dependency_overrides.pop(get_redis, None)
    assert not got_response, "Server should have rejected the bad token"


def test_ws_endpoint_accepts_valid_token_and_sends_ready():
    mock_db = make_mock_db()
    mock_redis = make_mock_redis()

    async def override_db():
        yield mock_db

    with patch("app.services.tour_stream.token_service.verify_access_token", return_value=str(uuid.uuid4())), \
         patch("app.services.tour_stream.SessionService") as mock_svc_cls:
        mock_svc = AsyncMock()
        mock_svc.create = AsyncMock(return_value="session-id-1")
        mock_svc.end_session = AsyncMock()
        mock_svc_cls.return_value = mock_svc

        app.dependency_overrides[get_db] = override_db
        app.dependency_overrides[get_redis] = lambda: mock_redis
        try:
            with TestClient(app) as client:
                with client.websocket_connect("/route/ws") as ws:
                    ws.send_json({"token": "valid-token"})
                    msg = ws.receive_json()
                    assert msg["type"] == "ready"
                    assert "route_id" in msg
        finally:
            app.dependency_overrides.pop(get_db, None)
            app.dependency_overrides.pop(get_redis, None)
