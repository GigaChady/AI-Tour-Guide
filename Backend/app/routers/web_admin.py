from fastapi import APIRouter, Depends
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.dependencies import get_current_admin
from app.core.config import DEFAULT_SYSTEM_CONFIG, LLM_PROVIDERS, TTS_PROVIDERS
from app.models.models import Route, SystemConfig, User
from app.schemas.schemas import (
    AdminDeploymentUser,
    AdminDeploymentsResponse,
    AdminStatsResponse,
    AvailableProvidersResponse,
    ProviderOption,
    SystemConfigResponse,
    SystemConfigUpdateRequest,
)
from app.services.tts.factory import set_tts_provider

router = APIRouter(prefix="/web/admin", tags=["web-admin"])


@router.get("/deployments", response_model=AdminDeploymentsResponse, dependencies=[Depends(get_current_admin)])
async def get_deployments(
    db: AsyncSession = Depends(get_db),
):
    users_result = await db.execute(select(User))
    users = users_result.scalars().all()

    active_routes_result = await db.execute(
        select(Route).where(Route.ended_at.is_(None))
    )
    active_routes = active_routes_result.scalars().all()
    active_route_by_user = {str(r.user_id): r for r in active_routes}

    return AdminDeploymentsResponse(
        users=[
            AdminDeploymentUser(
                id=str(u.id),
                name=u.name,
                email=u.email,
                is_active=u.is_active,
                on_route=str(u.id) in active_route_by_user,
            )
            for u in users
        ]
    )


@router.get("/stats", response_model=AdminStatsResponse, dependencies=[Depends(get_current_admin)])
async def get_admin_stats(db: AsyncSession = Depends(get_db)):
    # result = await db.execute(select(func.count(Route.id)).where(Route.ended_at.is_(None)))
    result = await db.execute(select(func.count(User.id))) #TODO: Fix later
    active_users = result.scalar() or 0
    return AdminStatsResponse(active_users=active_users, token_spent=15000)


@router.get("/providers", response_model=AvailableProvidersResponse, dependencies=[Depends(get_current_admin)])
async def get_available_providers():
    return AvailableProvidersResponse(tts_providers=TTS_PROVIDERS, llm_providers=LLM_PROVIDERS)


@router.get("/config", response_model=SystemConfigResponse, dependencies=[Depends(get_current_admin)])
async def get_system_config(db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(SystemConfig))
    rows = {row.key: row.value for row in result.scalars().all()}
    return SystemConfigResponse(
        tts_provider=rows.get("tts_provider", DEFAULT_SYSTEM_CONFIG["tts_provider"]),
        llm_provider=rows.get("llm_provider", DEFAULT_SYSTEM_CONFIG["llm_provider"]),
    )


@router.put("/config", response_model=SystemConfigResponse, dependencies=[Depends(get_current_admin)])
async def update_system_config(body: SystemConfigUpdateRequest, db: AsyncSession = Depends(get_db)):
    if body.tts_provider is not None:
        row = await db.get(SystemConfig, "tts_provider")
        if row:
            row.value = body.tts_provider
        else:
            db.add(SystemConfig(key="tts_provider", value=body.tts_provider))
        set_tts_provider(body.tts_provider)

    if body.llm_provider is not None:
        row = await db.get(SystemConfig, "llm_provider")
        if row:
            row.value = body.llm_provider
        else:
            db.add(SystemConfig(key="llm_provider", value=body.llm_provider))

    await db.commit()
    return await get_system_config(db)
