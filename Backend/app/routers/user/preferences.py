
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.core.dependencies import get_current_user
from app.models.models import User, UserPreferences
from app.schemas.schemas import UserPreferencesSchema

router = APIRouter(prefix="/user", tags=["user"])

@router.get("/preferences", response_model=UserPreferencesSchema)
async def get_preferences(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    result = await db.execute(select(UserPreferences).where(UserPreferences.user_id == current_user.id))
    user_prefs = result.scalar()
    if not user_prefs:
        raise HTTPException(status_code=404, detail="Preferences not found")
    return user_prefs

@router.put("/preferences", status_code=status.HTTP_204_NO_CONTENT)
async def update_preferences(
    prefs: dict,  
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    result = await db.execute(select(UserPreferences).where(UserPreferences.user_id == current_user.id))
    user_prefs = result.scalar()
    if not user_prefs:
        user_prefs = UserPreferences(user_id=current_user.id, interests=[])
        db.add(user_prefs)
    for k, v in prefs.items():
        setattr(user_prefs, k, v)
    await db.commit()
    return
