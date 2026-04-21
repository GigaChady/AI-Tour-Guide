from app.schemas.schemas import NarrationSettingsRequest
from fastapi import APIRouter, Depends, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.dependencies import get_current_user
from app.models.models import User, UserNarrationSettings

router = APIRouter(prefix="/user", tags=["user"])

@router.post("/narration-settings", status_code=status.HTTP_204_NO_CONTENT)
async def save_narration_settings(
    body: NarrationSettingsRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(select(UserNarrationSettings).where(UserNarrationSettings.user_id == current_user.id))
    ns = result.scalar_one_or_none()
    if not ns:
        ns = UserNarrationSettings(user_id=current_user.id, settings=body.model_dump())
        db.add(ns)
    else:
        ns.settings = body.model_dump()
    await db.commit()
    return
