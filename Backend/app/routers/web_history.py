from fastapi import APIRouter, Depends
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.dependencies import get_current_user
from app.models.models import Route, User
from app.schemas.schemas import RouteHistoryItem,RouteHistoryResponse, RouteHistoryUser, WebDashboardExpedition, WebDashboardResponse, WebDashboardStats



router = APIRouter(prefix="/web", tags=["web"])


@router.get("/history", response_model=RouteHistoryResponse)
async def get_route_history(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    stats_result = await db.execute(
        select(func.count(Route.id), func.sum(Route.distance_m)).where(
            Route.user_id == current_user.id
        )
    )
    total, total_distance_m = stats_result.one()
    total_distance_m = total_distance_m or 0.0

    routes_result = await db.execute(
        select(Route)
        .where(Route.user_id == current_user.id)
        .order_by(Route.started_at.desc())
    )
    routes = routes_result.scalars().all()

    total_duration_s = sum(
        int((r.ended_at - r.started_at).total_seconds()) if r.started_at and r.ended_at else 0
        for r in routes
    )

    return RouteHistoryResponse(
        user=RouteHistoryUser(
            name=current_user.name,
            avatar_url=current_user.avatar_url,
            total_explorations=total,
            total_distance_km=round(total_distance_m / 1000, 2),
            total_duration_minutes=total_duration_s // 60,
        ),
        routes=[
            RouteHistoryItem(
                id=str(r.id),
                name=r.name,
                date=r.started_at,
                distance_km=r.distance_m if r.distance_m else 0,
                duration_minutes=(int((r.ended_at - r.started_at).total_seconds()) // 60 if r.started_at and r.ended_at else 0),
                route_url=r.route_url,
            )
            for r in routes
        ],
    )


@router.get("/dashboard", response_model=WebDashboardResponse)
async def get_dashboard(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    stats_result = await db.execute(
        select(
            func.count(func.distinct(Route.country)),
            func.count(func.distinct(Route.city)),
            func.sum(Route.distance_m),
        ).where(Route.user_id == current_user.id)
    )
    total_countries, total_cities, total_distance_m = stats_result.one()
    total_distance_m = total_distance_m or 0.0

    routes_result = await db.execute(
        select(Route)
        .where(Route.user_id == current_user.id)
        .order_by(Route.started_at.desc())
    )
    routes = routes_result.scalars().all()

    total_duration_s = sum(
        int((r.ended_at - r.started_at).total_seconds()) if r.started_at and r.ended_at else 0
        for r in routes
    )

    return WebDashboardResponse(
        stats=WebDashboardStats(
            total_countries=total_countries or 0,
            total_cities=total_cities or 0,
            total_distance_km=round(total_distance_m / 1000, 2),
            total_duration_minutes=total_duration_s // 60,
        ),
        recent_expeditions=[
            WebDashboardExpedition(
                id=str(r.id),
                city=r.city,
                date=r.started_at,
                route_url=r.route_url,
            )
            for r in routes
        ],
    )
