import json
import uuid

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.models import UserPreferences
from app.services.session_service import SessionService


async def setup_tour_session(
    redis,
    db: AsyncSession,
    user_id: str,
    route_id: str,
) -> tuple[str, SessionService, object]:
    session_svc = SessionService(redis)
    session_id = await session_svc.create(user_id=user_id, route_id=route_id)

    result = await db.execute(
        select(UserPreferences).where(UserPreferences.user_id == uuid.UUID(user_id))
    )
    prefs = result.scalar_one_or_none()
    if prefs and prefs.interests:
        answer_keys = prefs.interests[0].get("answer_keys", []) if prefs.interests else []
        await redis.set(f"preferences:{session_id}", json.dumps(answer_keys))

    pubsub = redis.pubsub()
    await pubsub.subscribe(f"tour:{session_id}")

    return session_id, session_svc, pubsub


async def teardown_tour_session(
    redis,
    session_svc: SessionService,
    session_id: str,
    pubsub,
) -> None:
    if pubsub is not None:
        await pubsub.unsubscribe(f"tour:{session_id}")
        await pubsub.aclose()
    await redis.delete(f"preferences:{session_id}")
    await session_svc.end_session(session_id)
