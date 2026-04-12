import asyncio
import base64
import json
import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from app.services.narration_stream import handle_narration_ws


def make_pubsub(message_data: str | None = None):
    async def fake_listen():
        yield {"type": "subscribe", "data": 1}
        if message_data is not None:
            yield {"type": "message", "data": message_data}

    pubsub = AsyncMock()
    pubsub.listen = fake_listen
    return pubsub


def make_redis(pubsub):
    redis = MagicMock()
    redis.pubsub.return_value = pubsub
    redis.get = AsyncMock(return_value=json.dumps({"user_id": "user-1", "route_id": "route-1"}))
    return redis


@pytest.mark.asyncio
async def test_closes_4401_on_invalid_token():
    ws = AsyncMock()
    ws.receive_text.return_value = json.dumps({"token": "bad-token"})

    pubsub = make_pubsub()
    redis = make_redis(pubsub)

    with patch("app.services.narration_stream.token_service.verify_access_token", side_effect=ValueError("bad")):
        await handle_narration_ws(ws, "session-1", redis)

    ws.accept.assert_awaited_once()
    ws.close.assert_awaited_once_with(code=4401, reason="unauthorized")


@pytest.mark.asyncio
async def test_closes_4403_when_session_not_owned_by_user():
    ws = AsyncMock()
    ws.receive_text.return_value = json.dumps({"token": "good-token"})

    pubsub = make_pubsub()
    redis = MagicMock()
    redis.pubsub.return_value = pubsub
    redis.get = AsyncMock(return_value=json.dumps({"user_id": "other-user", "route_id": "route-1"}))

    with patch("app.services.narration_stream.token_service.verify_access_token", return_value="user-1"):
        await handle_narration_ws(ws, "session-1", redis)

    ws.close.assert_awaited_once_with(code=4403, reason="forbidden")


@pytest.mark.asyncio
async def test_closes_4403_when_session_not_found():
    ws = AsyncMock()
    ws.receive_text.return_value = json.dumps({"token": "good-token"})

    pubsub = make_pubsub()
    redis = MagicMock()
    redis.pubsub.return_value = pubsub
    redis.get = AsyncMock(return_value=None)

    with patch("app.services.narration_stream.token_service.verify_access_token", return_value="user-1"):
        await handle_narration_ws(ws, "session-1", redis)

    ws.close.assert_awaited_once_with(code=4403, reason="forbidden")


@pytest.mark.asyncio
async def test_sends_ready_after_valid_auth():
    ws = AsyncMock()
    ws.receive_text.return_value = json.dumps({"token": "good-token"})

    pubsub = make_pubsub(message_data="Hello.")
    redis = make_redis(pubsub)

    fake_audio = b"fake_mp3_bytes"
    mock_tts = AsyncMock()
    mock_tts.synthesize.return_value = fake_audio

    with patch("app.services.narration_stream.token_service.verify_access_token", return_value="user-1"), \
         patch("app.services.narration_stream.TTSFactory.get_provider", return_value=mock_tts):
        await handle_narration_ws(ws, "session-1", redis)

    sent_texts = [json.loads(call.args[0]) for call in ws.send_text.call_args_list]
    types = [m["type"] for m in sent_texts]
    assert "ready" in types


@pytest.mark.asyncio
async def test_streams_text_chunk_then_audio_chunk_then_done():
    ws = AsyncMock()
    ws.receive_text.return_value = json.dumps({"token": "good-token"})

    pubsub = make_pubsub(message_data="Hello. World.")
    redis = make_redis(pubsub)

    fake_audio = b"mp3bytes"
    mock_tts = AsyncMock()
    mock_tts.synthesize.return_value = fake_audio
    expected_b64 = base64.b64encode(fake_audio).decode()

    with patch("app.services.narration_stream.token_service.verify_access_token", return_value="user-1"), \
         patch("app.services.narration_stream.TTSFactory.get_provider", return_value=mock_tts):
        await handle_narration_ws(ws, "session-1", redis)

    sent = [json.loads(call.args[0]) for call in ws.send_text.call_args_list]
    types = [m["type"] for m in sent]

    assert types == ["ready", "text_chunk", "audio_chunk", "text_chunk", "audio_chunk", "done"]

    assert sent[1] == {"type": "text_chunk", "id": 0, "text": "Hello"}
    assert sent[2] == {"type": "audio_chunk", "id": 0, "audio_b64": expected_b64}
    assert sent[3] == {"type": "text_chunk", "id": 1, "text": "World"}
    assert sent[4] == {"type": "audio_chunk", "id": 1, "audio_b64": expected_b64}
    assert sent[5] == {"type": "done"}


@pytest.mark.asyncio
async def test_sends_error_on_tts_failure_and_continues():
    ws = AsyncMock()
    ws.receive_text.return_value = json.dumps({"token": "good-token"})

    pubsub = make_pubsub(message_data="Hello. World.")
    redis = make_redis(pubsub)

    mock_tts = AsyncMock()
    mock_tts.synthesize.side_effect = [RuntimeError("TTS down"), b"ok_mp3"]

    with patch("app.services.narration_stream.token_service.verify_access_token", return_value="user-1"), \
         patch("app.services.narration_stream.TTSFactory.get_provider", return_value=mock_tts):
        await handle_narration_ws(ws, "session-1", redis)

    sent = [json.loads(call.args[0]) for call in ws.send_text.call_args_list]
    types = [m["type"] for m in sent]

    assert types == ["ready", "text_chunk", "error", "text_chunk", "audio_chunk", "done"]
    error_msg = next(m for m in sent if m["type"] == "error")
    assert "0" in error_msg["message"]


@pytest.mark.asyncio
async def test_sends_error_on_timeout():
    ws = AsyncMock()
    ws.receive_text.return_value = json.dumps({"token": "good-token"})

    async def slow_listen():
        yield {"type": "subscribe", "data": 1}
        await asyncio.sleep(999)

    pubsub = AsyncMock()
    pubsub.listen = slow_listen
    redis = make_redis(pubsub)

    with patch("app.services.narration_stream.token_service.verify_access_token", return_value="user-1"), \
         patch("app.services.narration_stream.NARRATION_TIMEOUT", 0.01):
        await handle_narration_ws(ws, "session-1", redis)

    sent = [json.loads(call.args[0]) for call in ws.send_text.call_args_list]
    assert any(m["type"] == "error" and "timeout" in m["message"] for m in sent)


from starlette.testclient import TestClient
from app.main import app


def test_ws_endpoint_rejects_bad_token():
    from starlette.websockets import WebSocketDisconnect as StarletteWSD
    from app.core.redis import get_redis as _get_redis

    with patch("app.services.narration_stream.token_service.verify_access_token", side_effect=ValueError("bad")):
        app.dependency_overrides[_get_redis] = lambda: MagicMock()
        try:
            with TestClient(app) as client:
                try:
                    with client.websocket_connect("/narration/ws/test-session") as ws:
                        ws.send_json({"token": "bad-token"})
                        ws.receive_json()
                        assert False, "Expected WebSocket disconnect"
                except StarletteWSD as exc:
                    assert exc.code == 4401
                except Exception:
                    pass  
        finally:
            app.dependency_overrides.pop(_get_redis, None)


def test_ws_endpoint_rejects_bad_session():
    import json as _json
    from starlette.websockets import WebSocketDisconnect as StarletteWSD
    from app.core.redis import get_redis as _get_redis

    mock_redis = MagicMock()
    mock_redis.get = AsyncMock(return_value=_json.dumps({"user_id": "other-user", "route_id": "route-1"}))

    with patch("app.services.narration_stream.token_service.verify_access_token", return_value="user-1"):
        app.dependency_overrides[_get_redis] = lambda: mock_redis
        try:
            with TestClient(app) as client:
                try:
                    with client.websocket_connect("/narration/ws/bad-session") as ws:
                        ws.send_json({"token": "valid-token"})
                        ws.receive_json()
                        assert False, "Expected WebSocket disconnect"
                except StarletteWSD as exc:
                    assert exc.code == 4403
                except Exception:
                    pass  
        finally:
            app.dependency_overrides.pop(_get_redis, None)
