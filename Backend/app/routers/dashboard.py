import asyncio
import json
import uuid

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.dependencies import get_current_user
from app.core.redis import get_redis
from app.models.models import User, UserPreferences
from app.schemas.schemas import DashboardPOIResponse, Location
from app.services.session_service import SessionService

router = APIRouter(prefix="/route/dashboard", tags=["dashboard"])

_POI_TIMEOUT_SECONDS = 30


@router.post("/poi", response_model=DashboardPOIResponse)
async def get_dashboard_poi(
    body: Location,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    redis=Depends(get_redis),
):
    user_id = str(current_user.id)
    session_svc = SessionService(redis)
    session_id = await session_svc.create(user_id=user_id, route_id="dashboard")

    result = await db.execute(
        select(UserPreferences).where(UserPreferences.user_id == current_user.id)
    )
    prefs = result.scalar_one_or_none()
    if prefs and prefs.interests:
        await redis.set(f"preferences:{session_id}", json.dumps(prefs.interests))

    pubsub = redis.pubsub() #
    await pubsub.subscribe(f"tour:{session_id}")

    await redis.xadd("location:events", {
        "session_id": session_id,
        "lat": str(body.lat),
        "lng": str(body.lng),
        "include_photos": "1",
    })

    poi = None
    try:
        poi = await asyncio.wait_for(
            _wait_for_poi(pubsub), # waiting for sth like {"type": "pois", "data": [{"name": "...", "photos": ["url1", "url2"], ...}]}
            timeout=_POI_TIMEOUT_SECONDS,
        )
    except asyncio.TimeoutError:
        pass
    finally:
        await pubsub.unsubscribe(f"tour:{session_id}")
        await pubsub.aclose()
        await redis.delete(f"preferences:{session_id}")
        await session_svc.end_session(session_id)

    if poi is None:
        raise HTTPException(status_code=504, detail="No POI received within timeout")
    if not poi:
        raise HTTPException(status_code=404, detail="Worker returned no POIs")

    return DashboardPOIResponse(poi=poi[0])


async def _wait_for_poi(pubsub) -> list: # can change for future 
    async for message in pubsub.listen():
        if message["type"] != "message":
            continue
        try:
            data = json.loads(message["data"])
            if data.get("type") == "pois":
                return data.get("data", [])
        except (json.JSONDecodeError, KeyError):
            continue
