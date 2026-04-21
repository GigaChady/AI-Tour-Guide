import json

from fastapi import APIRouter, Depends, WebSocket 
# from fastapi import HTTPException
# from fastapi.responses import Response
# from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
# from app.core.config import settings

from app.core.database import get_db
# from app.core.dependencies import get_current_user
from app.core.redis import get_redis
# from app.models.models import User, UserPreferences
# from app.schemas.schemas import DashboardJobResponse, ErrorResponse, Location, LocationEvent, PoiData, UserPreferencesCache
# from app.services.session_service import SessionService
from app.services.dashboard_stream import handle_dashboard_ws

router = APIRouter(prefix="/route", tags=["dashboard"])


@router.websocket("/ws")
async def dashboard_ws(
    websocket: WebSocket,
    db: AsyncSession = Depends(get_db),
    redis=Depends(get_redis),
):
    '''we enqueue a job to generate a POI for dasboard from location data'''
    user_id = str(current_user.id)
    session_svc = SessionService(redis)
    session_id = await session_svc.create(user_id=user_id, route_id="dashboard")
    await redis.expire(f"session:{session_id}:meta", settings.RESULT_TTL) # session for metadata

    result = await db.execute(
        select(UserPreferences).where(UserPreferences.user_id == current_user.id)
    )
    prefs = result.scalar_one_or_none()
    if prefs and prefs.interests:
        answer_keys = prefs.interests[0].get("answer_keys", []) if prefs.interests else []
        await redis.set(f"preferences:{session_id}", json.dumps(answer_keys), ex=settings.RESULT_TTL)

    await redis.xadd("location:events", {
        "session_id": session_id,
        "lat": str(body.lat),
        "lng": str(body.lng),
        "include_photos": "2", # i want 2 photos for dashboard
    })

    return DashboardJobResponse(session_id=session_id)


# @router.post("/poi", response_model=DashboardJobResponse, status_code=202)
# async def enqueue_dashboard_poi(
#     body: Location,
#     current_user: User = Depends(get_current_user),
#     db: AsyncSession = Depends(get_db),
#     redis=Depends(get_redis),
# ):
#     '''we enqueue a job to generate a POI for dasboard from location data'''
#     user_id = str(current_user.id)
#     session_svc = SessionService(redis)
#     session_id = await session_svc.create(user_id=user_id, route_id="dashboard")
#     await redis.expire(f"session:{session_id}:meta", settings.RESULT_TTL) 

#     result = await db.execute(
#         select(UserPreferences).where(UserPreferences.user_id == current_user.id)
#     )
#     prefs = result.scalar_one_or_none()
#     if prefs and prefs.interests:
#         cache = UserPreferencesCache(interests=prefs.interests)
#         await redis.set(f"preferences:{session_id}", cache.model_dump_json(), ex=settings.RESULT_TTL)

#     event = LocationEvent(session_id=session_id, lat=body.lat, lng=body.lng, include_photos=2)
#     await redis.xadd("location:events", {k: str(v) for k, v in event.model_dump(exclude_none=False).items()})

#     return DashboardJobResponse(session_id=session_id)


# @router.get("/poi/{session_id}", response_model=PoiData, responses={404: {"model": ErrorResponse}})
# async def poll_dashboard_poi(
#     session_id: str,
#     current_user: User = Depends(get_current_user),
#     redis=Depends(get_redis),
# ):
#     session_svc = SessionService(redis)
#     if not await session_svc.verify(session_id, str(current_user.id)):
#         raise HTTPException(status_code=404, detail="Session not found")

#     raw = await redis.get(f"result:{session_id}")
#     if raw is None:
#         return Response(status_code=202) 

#     pois = json.loads(raw)
#     await redis.delete(f"result:{session_id}", f"preferences:{session_id}")
#     await session_svc.end_session(session_id)

#     if not pois:
#         raise HTTPException(status_code=404, detail="Worker returned no POIs")

#     return PoiData(**pois[0])
