import uuid

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.dependencies import get_current_user
from app.models.models import Route, RoutePoi, User
from app.schemas.schemas import PlanerPoisResponse, PlanerSaveRequest, RoutePoiResponse


router = APIRouter(prefix="/web/planer", tags=["planer"])


@router.post("", response_model=None, status_code=204)
async def save_planer_route(
    body: PlanerSaveRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    route = Route(
        user_id=current_user.id,
        name=body.name,
    )
    db.add(route)
    await db.flush()

    for poi in body.pois:
        db.add(RoutePoi(
            route_id=route.id,
            poi_id=poi.poi_id,
            name=poi.name,
            lat=poi.lat,
            lng=poi.lng,
            description=poi.description,
        ))

    await db.commit()


@router.get("/{route_id}", response_model=PlanerPoisResponse)
async def get_planer_pois(
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

    await db.refresh(route, ["pois"])

    return PlanerPoisResponse(
        pois=[
            RoutePoiResponse(
                id=str(p.id),
                poi_id=p.poi_id,
                name=p.name,
                lat=p.lat,
                lng=p.lng,
                description=p.description,
            )
            for p in route.pois
        ]
    )
