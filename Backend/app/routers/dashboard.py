import json

from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import Response
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.config import settings

from app.core.database import get_db
from app.core.dependencies import get_current_user
from app.core.redis import get_redis
from app.models.models import User, UserPreferences
from app.schemas.schemas import DashboardJobResponse, DashboardPOIResponse, Location
from app.services.session_service import SessionService
router = APIRouter(prefix="/route/dashboard", tags=["dashboard"])


@router.post("/poi", response_model=DashboardJobResponse, status_code=202)
async def enqueue_dashboard_poi(
    body: Location,
    current_user: User = Depends(get_current_user),
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


@router.get("/poi/{session_id}", response_model=DashboardPOIResponse)
async def poll_dashboard_poi(
    session_id: str,
    current_user: User = Depends(get_current_user),
    redis=Depends(get_redis),
):
    session_svc = SessionService(redis)
    if not await session_svc.verify(session_id, str(current_user.id)):
        raise HTTPException(status_code=404, detail="Session not found")

    raw = await redis.get(f"result:{session_id}")
    if raw is None:
        return Response(status_code=202) # still processing, no result yet

    pois = json.loads(raw) # tu jeszcze do ustalenia format zdj czy url czy base64
    await redis.delete(f"result:{session_id}", f"preferences:{session_id}")
    await session_svc.end_session(session_id)
    '''Na razie zakladam cos w stylu
    {
        "poi": {
            "name": "Rynek we Wrocławiu",
            "photos": [
            "https://example.com/rynek1.jpg",
            "https://example.com/rynek2.jpg"       i tu zalezy czy chcemy url czy base64
            ],
            "photos_base64": [
            "iVBORw0KGgoAAAANSUhEUgAA...",
            "iVBORw0KGgoAAAANSUhEUgAA..."
            ],
            "description": "Główny plac Wrocławia"
        }
    }
    '''
    if not pois:
        raise HTTPException(status_code=404, detail="Worker returned no POIs")

    return DashboardPOIResponse(poi=pois[0])
