from typing import Any
from app.schemas.schemas import NarrationSettingsRequest
from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.dependencies import get_current_user
from app.models.models import User

router = APIRouter(prefix="/user", tags=["user"])


@router.post("/narration-settings", status_code=status.HTTP_204_NO_CONTENT)
async def save_narration_settings(
    settings: NarrationSettingsRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    current_user.narration_settings = settings.dict()
    await db.commit()
    return
