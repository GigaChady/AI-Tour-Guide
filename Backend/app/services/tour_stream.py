import asyncio
import base64
import json
import uuid
from datetime import datetime, timezone

from fastapi import WebSocket, WebSocketDisconnect
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings, DEFAULT_NARRATION, GRACE_SECONDS
from app.schemas.schemas import NarrationChunk, NarrationDone, NarrationTranscript, NarrationTranscriptChunk, PoiData, PoisMessage, NarrationMessage, Location, LocationEvent, WsConnectRequest, WsReadyMessage, WsReconnectedMessage, WorkerMessage, ErrorResponse
from app.core.database import AsyncSessionLocal
from app.core.redis import get_redis
from app.models.models import Route, RoutePoi, UserNarrationSettings
from app.services.session_helpers import setup_tour_session, teardown_tour_session
from app.services.session_service import SessionService
from app.services.token_service import token_service
from app.services.tts.chunker import split as chunk_text
from app.services.tts.factory import TTSFactory



async def handle_tour_ws(
    websocket: WebSocket,
    db: AsyncSession,
    redis,
) -> None:
    await websocket.accept()

    try:
        raw = await websocket.receive_text()  # {"token": "...", "session_id": "..."} or {"token": "..."}
        data = json.loads(raw)
        req = WsConnectRequest(**data)
        token = req.token
        user_id = token_service.verify_access_token(token)
        incoming_session_id = req.session_id
    except WebSocketDisconnect:
        return
    except (json.JSONDecodeError, KeyError, ValueError):
        await websocket.send_text(ErrorResponse(detail="unauthorized").model_dump_json())
        await websocket.close(code=4401)
        return

    result = await db.execute(
        select(UserNarrationSettings).where(UserNarrationSettings.user_id == uuid.UUID(user_id))
    )
    ns = result.scalar_one_or_none()
    narration_cfg = (ns.settings or {}) if ns else {}

    pubsub = None
    session_svc = None
    session_id = None
    route_id = None
    '''when we lost connection, we set a grace key in redis and wait for the client to reconnect period 
    if client recoonects we remove grace key and restore session if client doesn't reconnect in time, we finalize route and cleanup session'''
    if incoming_session_id:
        result = await _try_reconnect(redis, user_id, incoming_session_id)
        if result is None:
            await websocket.send_text(ErrorResponse(detail="session not found or expired").model_dump_json())
            await websocket.close(code=4404)
            return
        session_id, session_svc, pubsub, route_id = result
    else:
        try:
            route = Route(
                user_id=uuid.UUID(user_id),
                started_at=datetime.now(timezone.utc).replace(tzinfo=None),
            )
            db.add(route)
            await db.commit()
            await db.refresh(route)
            route_id = str(route.id)
        except Exception:
            await websocket.send_text(ErrorResponse(detail="internal error").model_dump_json())
            await websocket.close(code=4500)
            return

    disconnected = False
    disconnect_event = asyncio.Event()

    try:
        if incoming_session_id:
            await websocket.send_text(WsReconnectedMessage(
                type="reconnected", route_id=route_id, session_id=session_id,
            ).model_dump_json())
        else:
            session_id, session_svc, pubsub = await setup_tour_session(
                redis, db, user_id, route_id=route_id
            )
            await websocket.send_text(WsReadyMessage(
                type="ready", route_id=route_id, session_id=session_id,
            ).model_dump_json())

        client_task = asyncio.create_task(
            _handle_client_messages(websocket, db, redis, session_id, route_id, disconnect_event)
        )
        worker_task = asyncio.create_task(
            _handle_worker_messages(websocket, pubsub, route_id, narration_cfg)
        )

        done, pending = await asyncio.wait(
            [client_task, worker_task],
            return_when=asyncio.FIRST_COMPLETED,
        )
        for task in pending:
            task.cancel()
            try:
                await task
            except (asyncio.CancelledError, Exception):
                pass

        disconnected = disconnect_event.is_set() or any(
            not t.cancelled() and isinstance(t.exception(), WebSocketDisconnect)
            for t in done
        )

    except WebSocketDisconnect:
        disconnected = True
    finally:
        if disconnected and session_id is not None:
            if pubsub is not None:
                try:
                    await pubsub.unsubscribe(f"tour:{session_id}")
                    await pubsub.aclose()
                except Exception:
                    pass
            await redis.set(f"session:{session_id}:grace", "1")
            asyncio.create_task(_grace_period_cleanup(session_id, route_id))
        else:
            await _finalize_route(db, route_id)
            if session_svc is not None and session_id is not None:
                await teardown_tour_session(redis, session_svc, session_id, pubsub)


async def _try_reconnect(redis, user_id: str, session_id: str):
    session_svc = SessionService(redis)
    meta = await session_svc.get_meta(session_id)
    if not meta or meta.user_id != user_id:
        return None

    deleted = await redis.delete(f"session:{session_id}:grace")
    if not deleted:
        return None 

    pubsub = redis.pubsub()
    await pubsub.subscribe(f"tour:{session_id}")
    route_id = meta.route_id
    return session_id, session_svc, pubsub, route_id


async def _grace_period_cleanup(session_id: str, route_id: str) -> None:
    await asyncio.sleep(GRACE_SECONDS)
    redis = get_redis()
    deleted = await redis.delete(f"session:{session_id}:grace")
    if deleted: 
        async with AsyncSessionLocal() as db:
            await _finalize_route(db, route_id)
        session_svc = SessionService(redis)
        await teardown_tour_session(redis, session_svc, session_id, pubsub=None)


async def _handle_client_messages(
    websocket: WebSocket,
    db: AsyncSession,
    redis,
    session_id: str,
    route_id: str,
    disconnect_event: asyncio.Event,
) -> None:
    while True:
        try:
            raw = await asyncio.wait_for(
                websocket.receive_text(),
                timeout=settings.STREAM_TIMEOUT_SECONDS,
            )
        except asyncio.TimeoutError:
            try:
                await websocket.send_text(json.dumps({"type": "session_ended", "reason": "timeout"}))
            except Exception:
                pass
            return
        except WebSocketDisconnect:
            disconnect_event.set()
            return
        try:
            data = json.loads(raw)  # {"lat": 50.123, "lng": 19.456}
            loc = Location(**data)
            await _save_location(db, route_id, lat=loc.lat, lng=loc.lng)
            event = LocationEvent(session_id=session_id, lat=loc.lat, lng=loc.lng, include_photos=1)
            await redis.xadd("location:events", {k: str(v) for k, v in event.model_dump().items()})
        except (json.JSONDecodeError, KeyError, TypeError, ValueError):
            pass


async def _handle_worker_messages(websocket: WebSocket, pubsub, route_id: str, narration_cfg: dict) -> None:
    async for message in pubsub.listen():
        if message["type"] != "message":
            continue
        try:
            data = WorkerMessage(**json.loads(message["data"]))
            if data.type == "pois":
                poi_list = data.data or []
                msg = PoisMessage(
                    type="pois",
                    data=[PoiData(**poi) for poi in poi_list]
                )
                await websocket.send_text(msg.model_dump_json())
                await _save_pois(route_id, poi_list)
            elif data.type == "narration":
                msg = NarrationMessage(type="narration", text=data.text or "")
                await _stream_narration(websocket, msg.text, narration_cfg)
        except WebSocketDisconnect:
            raise
        except Exception:
            pass


async def _save_pois(route_id: str, poi_list: list) -> None:
    async with AsyncSessionLocal() as db:
        for poi in poi_list:
            photos = poi.get("photos", [])
            db.add(RoutePoi(
                route_id=uuid.UUID(route_id),
                poi_id=poi.get("id"),
                name=poi.get("name", ""),
                lat=float(poi.get("lat", 0)),
                lng=float(poi.get("lng", 0)),
                description=poi.get("desc"),
                image_url=photos[0] if photos else None,
            ))
        await db.commit()


async def _stream_narration(websocket: WebSocket, text: str, narration_cfg: dict) -> None:
    chunks = chunk_text(text)
    if not chunks:
        return

    try:
        tts = TTSFactory.get_provider()
    except Exception:
        await websocket.send_text(ErrorResponse(detail="TTS provider unavailable").model_dump_json())
        return

    cfg = DEFAULT_NARRATION | narration_cfg

    await websocket.send_text(NarrationTranscript(
        type="narration_transcript",
        transcript=[NarrationTranscriptChunk(chunk_id=cid, text=sentence) for cid, sentence in chunks],
    ).model_dump_json())

    async def _synth(chunk_id: int, sentence: str):
        try:
            result = await tts.synthesize(
                sentence,
                language=cfg["language"],
                speed=cfg["speed"] * 10,
                pitch=cfg["pitch"],
                loudness=cfg["volume"],
            )
            return chunk_id, result.audio, result.words
        except Exception:
            return chunk_id, None, []

    tasks = [asyncio.create_task(_synth(cid, s)) for cid, s in chunks]
    try:
        for coro in asyncio.as_completed(tasks):
            chunk_id, audio, words = await coro
            if audio is None:
                continue
            await websocket.send_text(NarrationChunk(
                type="narration_chunk",
                chunk_id=chunk_id,
                audio=base64.b64encode(audio).decode(),
                words=words,
            ).model_dump_json())
        await websocket.send_text(NarrationDone(type="narration_done").model_dump_json())
    finally:
        for t in tasks:
            t.cancel()


async def _save_location(db: AsyncSession, route_id: str, lat: float, lng: float) -> None:
    result = await db.execute(
        text("SELECT ST_NPoints(path) FROM routes WHERE id = :route_id")
        .bindparams(route_id=route_id)
    )
    n_points = result.scalar()

    if n_points is None:
        await db.execute(
            text(
                "UPDATE routes SET path = ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) "
                "WHERE id = :route_id"
            ).bindparams(lng=lng, lat=lat, route_id=route_id)
        )
    elif n_points == 1:
        await db.execute(
            text(
                "UPDATE routes SET path = ST_MakeLine(path, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) "
                "WHERE id = :route_id"
            ).bindparams(lng=lng, lat=lat, route_id=route_id)
        )
    else:
        await db.execute(
            text(
                "UPDATE routes SET path = ST_AddPoint(path, ST_MakePoint(:lng, :lat)) "
                "WHERE id = :route_id"
            ).bindparams(lng=lng, lat=lat, route_id=route_id)
        )
    await db.commit()


async def _finalize_route(db: AsyncSession, route_id: str) -> None:
    result = await db.execute(
        text("SELECT ST_NPoints(path) FROM routes WHERE id = :route_id")
        .bindparams(route_id=route_id)
    )
    n_points = result.scalar()

    if n_points and n_points >= 2:
        route = await db.get(Route, uuid.UUID(route_id))
        if route:
            route.ended_at = datetime.now(timezone.utc).replace(tzinfo=None)
            result = await db.execute(
                text(
                    "SELECT ST_Length(ST_GeogFromWKB(ST_AsBinary(path))) "
                    "FROM routes WHERE id = :route_id"
                ).bindparams(route_id=route_id)
            )
            route.distance_m = result.scalar()
            await db.commit()