import asyncio
import json
import uuid
from datetime import datetime, timezone

from fastapi import WebSocket, WebSocketDisconnect
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.models.models import Route, UserPreferences
from app.services.session_service import SessionService
from app.services.token_service import token_service
from app.services.tts import audio_pipeline
from app.services.tts.factory import TTSFactory

# TODO: edge casey + maybe adding heartbeat pingpong for keeping connection 

async def handle_tour_ws(
    websocket: WebSocket,
    db: AsyncSession,
    redis,
) -> None:
    await websocket.accept()

    try:
        raw = await websocket.receive_text()# expecting {"token": "access-token"}
        data = json.loads(raw)
        token = data.get("token", "")
        user_id = token_service.verify_access_token(token)
    except WebSocketDisconnect:
        return
    except (json.JSONDecodeError, KeyError, ValueError):
        await websocket.close(code=4401, reason="unauthorized")
        return

    try:
        route = Route( # initialize route with dummy point, will be updated with real points as they come in
            user_id=uuid.UUID(user_id),
            started_at=datetime.now(timezone.utc).replace(tzinfo=None),
        )
        db.add(route)
        await db.commit()
        await db.refresh(route)
        route_id = str(route.id)
    except Exception:
        await websocket.close(code=4500, reason="internal error")
        return

    pubsub = None 
    session_svc = SessionService(redis) 
    session_id = await session_svc.create(user_id=user_id, route_id=route_id)

    result = await db.execute(
        select(UserPreferences).where(UserPreferences.user_id == uuid.UUID(user_id))
    )
    prefs = result.scalar_one_or_none()
    if prefs and prefs.interests:
        await redis.set(f"preferences:{session_id}", json.dumps(prefs.interests))

    try:
        pubsub = redis.pubsub() # create pubsub connection for this websocket
        await pubsub.subscribe(f"tour:{session_id}") # subscribe to tour channel for this session to receive messages from worker

        await websocket.send_text(json.dumps({ # signal client is ready to receive messages and start tour 
            "type": "ready",
            "route_id": route_id,
        }))

        #rownolegle odpalam dwa taski - jeden do odbierania wiadomości od klienta (lokalizacja), drugi do odbierania wiadomości od workerów (POI, narracja)
        client_task = asyncio.create_task(
            _handle_client_messages(websocket, db, redis, session_id, route_id)
        )
        worker_task = asyncio.create_task(
            _handle_worker_messages(websocket, pubsub)
        )

        done, pending = await asyncio.wait( # czeka aż któryś z tasków się zakończy (np. klient rozłączy się, lub wystąpi błąd)
            [client_task, worker_task],
            return_when=asyncio.FIRST_COMPLETED,
        ) # po zakończeniu jednego tasku, drugi jest anulowany
        for task in pending:
            task.cancel()
            try:
                await task
            except (asyncio.CancelledError, Exception):
                pass

    except WebSocketDisconnect:
        pass
    finally: 
        '''# cleanup po rozłączeniu klienta lub błędzie
        odsubskrybowanie, oznaczenie trasy jako zakończonej, usunięcie preferencji z Redis i zakończenie sesji 
        potem mozna cos jeszcze dodać jak np. zapisanie trasy do historii użytkownika czy coś takiego
        i jak np usera wyrzuci na jakis czas to jest opcja powrotu do tej samej trasy jak się ponownie zaloguje, ale to już na później'''
        if pubsub is not None:
            await pubsub.unsubscribe(f"tour:{session_id}")
            await pubsub.aclose()
        await _finalize_route(db, route_id)
        await redis.delete(f"preferences:{session_id}")
        await session_svc.end_session(session_id)


async def _handle_client_messages(
    websocket: WebSocket,
    db: AsyncSession,
    redis,
    session_id: str,
    route_id: str,
) -> None:
    while True:
        try:
            raw = await asyncio.wait_for(
                websocket.receive_text(),
                timeout=settings.STREAM_TIMEOUT_SECONDS,
            )
        except asyncio.TimeoutError:
            break  # brak wiadomości od klienta przez STREAM_TIMEOUT_SECONDS → cleanup
        try:
            data = json.loads(raw) # oczekujemy wiadomości w formacie {"lat": 50.123, "lng": 19.456}
            lat = data.get("lat")
            lng = data.get("lng")
            if lat is not None and lng is not None:
                await _save_location(db, route_id, lat=float(lat), lng=float(lng)) # zapis do bazki
                await redis.xadd("location:events", {  # stream dla workera
                    "session_id": session_id,
                    "lat": str(lat),
                    "lng": str(lng),
                })
        except (json.JSONDecodeError, KeyError, TypeError, ValueError):
            pass 


async def _handle_worker_messages(websocket: WebSocket, pubsub) -> None: # receiving message from workers 
    async for message in pubsub.listen():
        if message["type"] != "message":
            continue
        try:
            data = json.loads(message["data"]) # oczekujemy wiadomości w formacie {"type": "pois", "data": [...] } lub {"type": "narration", "text": "..."}
            msg_type = data.get("type")
            if msg_type == "pois":
                await websocket.send_text(json.dumps({
                    "type": "pois",
                    "data": data.get("data", []),
                }))
            elif msg_type == "narration":
                await _stream_narration(websocket, data.get("text", ""))
        except WebSocketDisconnect:
            raise
        except Exception:
            pass


async def _stream_narration(websocket: WebSocket, text: str) -> None:
    try:
        tts = TTSFactory.get_provider()
    except Exception:
        await websocket.send_text(json.dumps({"type": "error", "message": "TTS provider unavailable"}))
        return

    synthesis = await audio_pipeline.synthesize(text, tts, language="en", speed=50, pitch=50, loudness=50)
    if synthesis is None:
        return

    await websocket.send_text(json.dumps({
        "type": "narration_transcript",
        "transcript": synthesis.transcript,
    }))

    try:
        result = await audio_pipeline.encode_hls(synthesis)
    except Exception:
        await websocket.send_text(json.dumps({"type": "error", "message": "HLS encoding failed"}))
        return

    await websocket.send_text(json.dumps({
        "type": "narration_ready",
        "hls_url": f"/audio/{result.narration_id}/index.m3u8",
    }))


async def _save_location(db: AsyncSession, route_id: str, lat: float, lng: float) -> None:
    result = await db.execute(
        text("SELECT ST_NPoints(path) FROM routes WHERE id = :route_id")
        .bindparams(route_id=route_id)
    )
    n_points = result.scalar()

    if n_points is None:
        await db.execute(
            text(
                "UPDATE routes SET path = "
                "ST_SetSRID(ST_GeomFromText('LINESTRING(' || :lng || ' ' || :lat || ')'), 4326) "
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
