import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, WebSocket
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text

from app.core.database import get_db
from app.core.dependencies import get_current_user
from app.core.redis import get_redis
from app.models.models import Route, RoutePoi, User
from app.services.tour_stream import handle_tour_ws

router = APIRouter(prefix="/route", tags=["route"])


@router.websocket("/ws")
async def tour_ws(
    websocket: WebSocket,
    db: AsyncSession = Depends(get_db),
    redis=Depends(get_redis),
):
    await handle_tour_ws(websocket, db, redis)


@router.get("/{route_id}/stats")
async def route_stats(
    route_id: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    try:
        rid = uuid.UUID(route_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Route not found.")

    route = await db.get(Route, rid)
    if not route or route.user_id != current_user.id:
        raise HTTPException(status_code=404, detail="Route not found.")

    distance_m = route.distance_m
    if distance_m is None:
        result = await db.execute(
            text(
                "SELECT ST_Length(ST_GeogFromWKB(ST_AsBinary(path))) "
                "FROM routes WHERE id = :id AND path IS NOT NULL"
            ).bindparams(id=route_id)
        )
        distance_m = result.scalar() or 0.0

    now = datetime.now(timezone.utc).replace(tzinfo=None)
    ended = route.ended_at or now
    duration_s = int((ended - route.started_at).total_seconds())

    result = await db.execute(
        select(RoutePoi).where(RoutePoi.route_id == rid)
    )
    pois = result.scalars().all()

    return {
        "distance_m": distance_m,
        "duration_s": duration_s,
        "started_at": route.started_at,
        "ended_at": route.ended_at,
        "pois": [
            {
                "id": str(p.id),
                "poi_id": p.poi_id,
                "name": p.name,
                "lat": p.lat,
                "lng": p.lng,
                "description": p.description,
            }
            for p in pois
        ],
    }
