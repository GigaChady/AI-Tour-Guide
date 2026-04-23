import asyncio
import json
import struct
import uuid
from datetime import datetime, timezone

from fastapi import WebSocket, WebSocketDisconnect
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings, DEFAULT_NARRATION, GRACE_SECONDS
from app.core.database import AsyncSessionLocal
from app.core.redis import get_redis
from app.models.models import Route, RoutePoi, UserNarrationSettings, UserPreferences
from app.schemas.schemas import (
    ErrorResponse, Location, LocationEvent, WsConnectRequest,
    WsPreviewReadyMessage, WsTourMessage,
    PoisMessage, PoiData, WorkerMessage, UserPreferencesCache,
    NarrationTranscript, NarrationTranscriptChunk, NarrationWords, NarrationDone,
)
from app.services.session_service import SessionService
from app.services.session_helpers import teardown_tour_session
from app.services.token_service import token_service
from app.services.tts.chunker import split as chunk_text
from app.services.tts.factory import TTSFactory

async def handle_dashboard_ws(websocket: WebSocket, db: AsyncSession, redis) -> None:
    await websocket.accept()

    try:
        raw = await websocket.receive_text()
        data = json.loads(raw) # {"token": "...", "session_id": "..."} session_id is optional, only for reconnecting to an existing tour session
        req = WsConnectRequest(**data)
        user_id = token_service.verify_access_token(req.token)
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

    session_svc = None
    session_id = None
    pubsub = None
    route_id = None
    mode = "preview"

    if incoming_session_id:
        reconnect = await _try_reconnect(redis, user_id, incoming_session_id)
        if reconnect is None:
            await websocket.send_text(ErrorResponse(detail="session not found or expired").model_dump_json())
            await websocket.close(code=4404)
            return
        session_id, session_svc, pubsub, route_id = reconnect
        mode = "tour"
    else:
        session_svc = SessionService(redis)
        session_id = await session_svc.create(user_id=user_id, route_id="dashboard")
        await _cache_preferences(db, redis, session_id, user_id)
        pubsub = redis.pubsub()
        await pubsub.subscribe(f"tour:{session_id}")

    state = {"mode": mode, "route_id": route_id}
    disconnect_event = asyncio.Event()

    try:
        if incoming_session_id:
            await websocket.send_text(WsTourMessage(
                route_id=route_id, session_id=session_id,
            ).model_dump_json())
        else:
            await websocket.send_text(WsPreviewReadyMessage(
                session_id=session_id,
            ).model_dump_json())

        client_task = asyncio.create_task(
            _handle_client(websocket, db, redis, session_id, session_svc, user_id, state, disconnect_event)
        )

        worker_task = asyncio.create_task(
            _handle_worker(websocket, pubsub, state, narration_cfg)
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

        disconnected = disconnect_event.is_set()

    except WebSocketDisconnect:
        disconnected = True
    finally:
        current_route_id = state["route_id"]
        if state["mode"] == "tour" and current_route_id is not None:
            if disconnected:
                if pubsub is not None:
                    try:
                        await pubsub.unsubscribe(f"tour:{session_id}")
                        await pubsub.aclose()
                    except Exception:
                        pass
                await redis.set(f"session:{session_id}:grace", "1")
                asyncio.create_task(_grace_cleanup(session_id, current_route_id))
            else:
                await _finalize_route(db, current_route_id)
                await teardown_tour_session(redis, session_svc, session_id, pubsub)
        else:
            await teardown_tour_session(redis, session_svc, session_id, pubsub)


# same as before
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


async def _cache_preferences(db: AsyncSession, redis, session_id: str, user_id: str) -> None:
    result = await db.execute(
        select(UserPreferences).where(UserPreferences.user_id == uuid.UUID(user_id))
    )
    prefs = result.scalar_one_or_none()
    if prefs and prefs.interests:
        cache = UserPreferencesCache(interests=prefs.interests)
        await redis.set(f"preferences:{session_id}", cache.model_dump_json())



async def _handle_client(
    websocket: WebSocket,
    db: AsyncSession,
    redis,
    session_id: str,
    session_svc: SessionService,
    user_id: str,
    state: dict,
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
            data = json.loads(raw)
        except (json.JSONDecodeError, ValueError):
            continue

        msg_type = data.get("type")

        if msg_type == "start_tour" and state["mode"] == "preview": 
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
                return
            state["mode"] = "tour"
            state["route_id"] = route_id
            await session_svc.update_route_id(session_id, route_id)
            await websocket.send_text(WsTourMessage(
                route_id=route_id, session_id=session_id,
            ).model_dump_json())

        elif msg_type == "end_tour" and state["mode"] == "tour":
            try:
                await websocket.send_text(json.dumps({"type": "session_ended", "reason": "user_ended"}))
            except Exception:
                pass
            return

        else:
            try:
                loc = Location(**data)
                include_photos = 1 if state["mode"] == "tour" else 2
                if state["mode"] == "tour" and state["route_id"]:
                    await _save_location(db, state["route_id"], lat=loc.lat, lng=loc.lng)
                event = LocationEvent(session_id=session_id, lat=loc.lat, lng=loc.lng, include_photos=include_photos)
                await redis.xadd("location:events", {k: str(v) for k, v in event.model_dump(exclude_none=True).items()})
            except (KeyError, TypeError, ValueError):
                pass


async def _handle_worker(websocket: WebSocket, pubsub, state: dict, narration_cfg: dict) -> None:
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
                if state["mode"] == "tour" and state["route_id"]:
                    await _save_pois(state["route_id"], poi_list)
            elif data.type == "narration" and state["mode"] == "tour":
                await _stream_narration(websocket, data.text or "", narration_cfg)
        except WebSocketDisconnect:
            raise
        except Exception:
            pass


async def _grace_cleanup(session_id: str, route_id: str) -> None:
    await asyncio.sleep(GRACE_SECONDS)
    redis = get_redis()
    deleted = await redis.delete(f"session:{session_id}:grace")
    if deleted:
        async with AsyncSessionLocal() as db:
            await _finalize_route(db, route_id)
        session_svc = SessionService(redis)
        await teardown_tour_session(redis, session_svc, session_id, pubsub=None)

#same as before
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


#same as before
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
            await websocket.send_bytes(struct.pack(">I", chunk_id) + audio) # binary frames instead of base64
            if words:
                await websocket.send_text(NarrationWords(
                    type="narration_words", chunk_id=chunk_id, words=words,
                ).model_dump_json())
        await websocket.send_text(NarrationDone(type="narration_done").model_dump_json())
    finally:
        for t in tasks:
            t.cancel()




# same as before
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

#same as before
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



