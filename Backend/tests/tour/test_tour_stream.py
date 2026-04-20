import asyncio
import json
import uuid
import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from app.services.tour_stream import (
    handle_tour_ws,
    _save_location,
    _finalize_route,
    _stream_narration,
    _handle_worker_messages,
)



def make_pubsub(messages: list[dict] | None = None):
    async def fake_listen():
        yield {"type": "subscribe", "data": 1}
        for msg in (messages or []):
            yield {"type": "message", "data": json.dumps(msg)}

    ps = AsyncMock()
    ps.listen = fake_listen
    return ps


def make_redis(pubsub, session_meta: dict | None = None):
    r = MagicMock()
    r.pubsub.return_value = pubsub
    r.get = AsyncMock(return_value=json.dumps(session_meta) if session_meta else None)
    r.set = AsyncMock()
    r.publish = AsyncMock()
    r.delete = AsyncMock()
    return r


def make_db(prefs_interests=None):
    db = AsyncMock()
    route_obj = MagicMock()
    route_obj.id = uuid.uuid4()
    db.refresh = AsyncMock()
    db.get = AsyncMock(return_value=route_obj)

    prefs_mock = MagicMock()
    prefs_mock.interests = prefs_interests
    scalar_result = MagicMock()
    scalar_result.scalar_one_or_none.return_value = prefs_mock if prefs_interests is not None else None
    scalar_none = MagicMock()
    scalar_none.scalar.return_value = None
    db.execute = AsyncMock(side_effect=[scalar_result, scalar_none, scalar_none, scalar_none])
    return db, route_obj



@pytest.mark.asyncio
async def test_closes_4401_on_invalid_token():
    ws = AsyncMock()
    ws.receive_text.return_value = json.dumps({"token": "bad"})
    db = AsyncMock()
    redis = make_redis(make_pubsub())

    with patch("app.services.tour_stream.token_service.verify_access_token", side_effect=ValueError("bad")):
        await handle_tour_ws(ws, db, redis)

    ws.accept.assert_awaited_once()
    ws.close.assert_awaited_once_with(code=4401, reason="unauthorized")


@pytest.mark.asyncio
async def test_closes_4500_on_db_error():
    ws = AsyncMock()
    ws.receive_text.return_value = json.dumps({"token": "good"})
    db = AsyncMock()
    db.add = MagicMock()
    db.commit = AsyncMock(side_effect=Exception("DB down"))
    redis = make_redis(make_pubsub())

    with patch("app.services.tour_stream.token_service.verify_access_token", return_value="user-1"):
        await handle_tour_ws(ws, db, redis)

    ws.close.assert_awaited_once_with(code=4500, reason="internal error")



@pytest.mark.asyncio
async def test_sends_ready_with_route_id():
    ws = AsyncMock()
    ws.receive_text.side_effect = [
        json.dumps({"token": "good"}),
        Exception("disconnect"),
    ]

    db, route_obj = make_db()
    pubsub = make_pubsub()
    redis = make_redis(pubsub)

    with patch("app.services.tour_stream.token_service.verify_access_token", return_value=str(uuid.uuid4())), \
         patch("app.services.tour_stream.SessionService") as mock_svc_cls:
        mock_svc = AsyncMock()
        mock_svc.create = AsyncMock(return_value="session-id-1")
        mock_svc.end_session = AsyncMock()
        mock_svc_cls.return_value = mock_svc

        await handle_tour_ws(ws, db, redis)

    sent = [json.loads(c.args[0]) for c in ws.send_text.call_args_list]
    ready_msgs = [m for m in sent if m.get("type") == "ready"]
    assert len(ready_msgs) == 1
    assert "route_id" in ready_msgs[0]



@pytest.mark.asyncio
async def test_save_location_creates_linestring_when_path_is_null():
    db = AsyncMock()
    scalar_none = MagicMock()
    scalar_none.scalar.return_value = None
    db.execute = AsyncMock(return_value=scalar_none)

    await _save_location(db, "route-uuid", lat=52.23, lng=21.01)

    first_call_sql = str(db.execute.call_args_list[0].args[0])
    assert "ST_NPoints" in first_call_sql
    second_call_sql = str(db.execute.call_args_list[1].args[0])
    assert "ST_GeomFromText" in second_call_sql or "LINESTRING" in second_call_sql
    db.commit.assert_awaited_once()


@pytest.mark.asyncio
async def test_save_location_adds_point_when_path_exists():
    db = AsyncMock()
    scalar_one = MagicMock()
    scalar_one.scalar.return_value = 1
    db.execute = AsyncMock(return_value=scalar_one)

    await _save_location(db, "route-uuid", lat=52.23, lng=21.01)

    second_call_sql = str(db.execute.call_args_list[1].args[0])
    assert "ST_AddPoint" in second_call_sql
    db.commit.assert_awaited_once()


@pytest.mark.asyncio
async def test_finalize_route_skips_when_fewer_than_2_points():
    db = AsyncMock()
    scalar_one = MagicMock()
    scalar_one.scalar.return_value = 1
    db.execute = AsyncMock(return_value=scalar_one)

    await _finalize_route(db, str(uuid.uuid4()))

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

    await _finalize_route(db, str(uuid.uuid4()))

    assert route_mock.ended_at is not None
    assert route_mock.distance_m == 1234.5
    db.commit.assert_awaited_once()



@pytest.mark.asyncio
async def test_stream_narration_sends_transcript_then_hls_url():
    ws = AsyncMock()

    mock_synthesis = MagicMock()
    mock_synthesis.transcript = [
        {"text": "Hello", "start": 0.0, "end": 2.0},
        {"text": "World", "start": 2.0, "end": 4.0},
    ]
    mock_result = MagicMock()
    mock_result.narration_id = "test-narration-id"
    mock_tts = AsyncMock()

    with patch("app.services.tour_stream.TTSFactory.get_provider", return_value=mock_tts), \
         patch("app.services.tour_stream.audio_pipeline.synthesize", return_value=mock_synthesis), \
         patch("app.services.tour_stream.audio_pipeline.encode_hls", return_value=mock_result):
        await _stream_narration(ws, "Hello. World.")

    sent = [json.loads(c.args[0]) for c in ws.send_text.call_args_list]
    types = [m["type"] for m in sent]
    assert types == ["narration_transcript", "narration_ready"]
    assert sent[0]["transcript"] == mock_synthesis.transcript
    assert sent[1]["hls_url"] == "/audio/test-narration-id/index.m3u8"



@pytest.mark.asyncio
async def test_worker_pois_message_forwarded_to_client():
    ws = AsyncMock()
    pois_data = [{"id": "poi-1", "name": "Museum"}]
    pubsub = make_pubsub([{"type": "pois", "data": pois_data}])

    with patch("app.services.tour_stream.TTSFactory.get_provider"):
        await _handle_worker_messages(ws, pubsub)

    sent = [json.loads(c.args[0]) for c in ws.send_text.call_args_list]
    assert sent[0] == {"type": "pois", "data": pois_data}


@pytest.mark.asyncio
async def test_worker_narration_message_triggers_hls_stream():
    ws = AsyncMock()
    pubsub = make_pubsub([{"type": "narration", "text": "Hello."}])

    mock_synthesis = MagicMock()
    mock_synthesis.transcript = [{"text": "Hello", "start": 0.0, "end": 2.0}]
    mock_result = MagicMock()
    mock_result.narration_id = "test-id"
    mock_tts = AsyncMock()

    with patch("app.services.tour_stream.TTSFactory.get_provider", return_value=mock_tts), \
         patch("app.services.tour_stream.audio_pipeline.synthesize", return_value=mock_synthesis), \
         patch("app.services.tour_stream.audio_pipeline.encode_hls", return_value=mock_result):
        await _handle_worker_messages(ws, pubsub)

    sent = [json.loads(c.args[0]) for c in ws.send_text.call_args_list]
    types = [m["type"] for m in sent]
    assert "narration_transcript" in types
    assert "narration_ready" in types
    assert "text_chunk" not in types
    assert "audio_chunk" not in types


@pytest.mark.asyncio
async def test_worker_unknown_message_type_is_ignored():
    ws = AsyncMock()
    pubsub = make_pubsub([{"type": "unknown_future_type", "data": "whatever"}])

    with patch("app.services.tour_stream.TTSFactory.get_provider"):
        await _handle_worker_messages(ws, pubsub)

    ws.send_text.assert_not_awaited()
