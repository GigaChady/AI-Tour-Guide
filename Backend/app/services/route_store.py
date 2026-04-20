import uuid
import logging
from datetime import datetime, timezone

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import AsyncSessionLocal
from app.models.models import Route, RoutePoi

logger = logging.getLogger(__name__)


async def create_route(db: AsyncSession, user_id: str) -> Route:
    route = Route(
        user_id=uuid.UUID(user_id),
        started_at=datetime.now(timezone.utc).replace(tzinfo=None),
    )
    db.add(route)
    await db.commit()
    await db.refresh(route)
    return route


async def save_location(db: AsyncSession, route_id: str, lat: float, lng: float) -> None:
    await db.execute(
        text("""
            UPDATE routes SET path = CASE
                WHEN path IS NULL
                    THEN ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)
                WHEN ST_NPoints(path) = 1
                    THEN ST_MakeLine(path, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
                ELSE
                    ST_AddPoint(path, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
            END
            WHERE id = :route_id
        """).bindparams(lng=lng, lat=lat, route_id=route_id)
    )
    await db.commit()


async def save_pois(route_id: str, poi_list: list) -> None:
    async with AsyncSessionLocal() as db:
        for poi in poi_list:
            db.add(RoutePoi(
                route_id=uuid.UUID(route_id),
                poi_id=poi.get("id"),
                name=poi.get("name", ""),
                lat=float(poi.get("lat", 0)),
                lng=float(poi.get("lng", 0)),
                description=poi.get("description"),
                image_url=poi.get("image_url"),
                image_base64=poi.get("image_base64"),
            ))
        await db.commit()


async def finalize_route(db: AsyncSession, route_id: str) -> None:
    result = await db.execute(
        text("SELECT ST_NPoints(path) FROM routes WHERE id = :route_id")
        .bindparams(route_id=route_id)
    )
    n_points = result.scalar()

    if not n_points or n_points < 2:
        return

    route = await db.get(Route, uuid.UUID(route_id))
    if not route:
        return

    route.ended_at = datetime.now(timezone.utc).replace(tzinfo=None)
    result = await db.execute(
        text(
            "SELECT ST_Length(ST_GeogFromWKB(ST_AsBinary(path))) "
            "FROM routes WHERE id = :route_id"
        ).bindparams(route_id=route_id)
    )
    route.distance_m = result.scalar()
    await db.commit()
