from datetime import datetime, timezone
from sqlalchemy import text
from fastapi import APIRouter, Depends, HTTPException
from app.core.dependencies import get_current_user, get_session_service
from app.schemas.schemas import RouteStartResponse, TrackPointRequest, RouteEditNameRequest, RouteEndRequest
from app.services.session_service import SessionService
from app.core.database import get_db
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.models import User, Route

router = APIRouter(prefix="/route", tags=["route"])


@router.post("/start", response_model=RouteStartResponse)
async def start_route(
    current_user: User = Depends(get_current_user),
    session: SessionService = Depends(get_session_service),
    db: AsyncSession = Depends(get_db),
):
    new_route = Route(
        user_id=current_user.id,
        started_at=datetime.now(timezone.utc),
    )
    db.add(new_route)
    await db.commit()
    await db.refresh(new_route)

    session_id = await session.create(user_id=str(current_user.id), route_id=str(new_route.id))

    return RouteStartResponse(session_id=session_id, route_id=str(new_route.id))


@router.post("/track")
async def track_point(
    body: TrackPointRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    sessions: SessionService = Depends(get_session_service),
):
    meta = await sessions.get_meta(body.session_id)
    if not meta or meta["user_id"] != str(current_user.id):
        raise HTTPException(403, "Session not found, expired, or does not belong to user.")

    route = await db.get(Route, meta["route_id"])
    if not route:
        raise HTTPException(404, "Route not found")

    if route.path is None:
        linestring = f"SRID=4326;LINESTRING({body.location.lng} {body.location.lat})"
        await db.execute(
            text("UPDATE routes SET path = ST_GeomFromText(:linestring) WHERE id = :route_id")
            .bindparams(linestring=linestring, route_id=str(route.id))
        )
    else:
        await db.execute(
            text("UPDATE routes SET path = ST_AddPoint(path, ST_MakePoint(:lng, :lat)) WHERE id = :route_id")
            .bindparams(lng=body.location.lng, lat=body.location.lat, route_id=str(route.id))
        )
    await db.commit()
    return {"ok": True}


@router.post("/end")
async def end_route(
    body: RouteEndRequest,
    current_user: User = Depends(get_current_user),
    session: SessionService = Depends(get_session_service),
    db: AsyncSession = Depends(get_db),
):
    meta = await session.get_meta(body.session_id)
    if not meta:
        raise HTTPException(403, "Session not found or expired.")

    route = await db.get(Route, meta["route_id"])
    if not route or route.user_id != current_user.id:
        raise HTTPException(404, "Route not found or not owned by user")

    result = await db.execute(
        text("SELECT ST_NPoints(path) FROM routes WHERE id = :route_id")
        .bindparams(route_id=str(route.id))
    )
    n_points = result.scalar()
    if n_points and n_points >= 2:
        route.ended_at = datetime.now(timezone.utc)
        result = await db.execute(
            text("SELECT ST_Length(ST_GeogFromWKB(ST_AsBinary(path))) FROM routes WHERE id = :route_id")
            .bindparams(route_id=str(route.id))
        )
        route.distance_m = result.scalar()
    await db.commit()
    await session.end_session(body.session_id)
    return {"ok": True}


@router.put("/edit-name")
async def edit_route_name(
    body: RouteEditNameRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    route = await db.get(Route, body.route_id)
    if not route or route.user_id != current_user.id:
        raise HTTPException(404, "Route not found")
    route.name = body.name
    await db.commit()
    await db.refresh(route)
    return {"ok": True, "route_id": str(route.id), "name": route.name}
