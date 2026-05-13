from app.schemas.schemas import NarrationSettingsRequest
from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import DEFAULT_NARRATION, NARRATION_TEST_TEXTS
from app.core.database import get_db
from app.core.dependencies import get_current_user
from app.models.models import User, UserNarrationSettings
from app.services.tts.factory import TTSFactory

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

@router.get("/narration-settings", status_code=status.HTTP_200_OK)
async def get_narration_settings(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(select(UserNarrationSettings).where(UserNarrationSettings.user_id == current_user.id))
    ns = result.scalar_one_or_none()
    if not ns:
        return {}
    return ns.settings


@router.post("/narration-settings/test")
async def test_narration(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(select(UserNarrationSettings).where(UserNarrationSettings.user_id == current_user.id))
    ns = result.scalar_one_or_none()
    narration_cfg = (ns.settings or {}) if ns else {}
    cfg = DEFAULT_NARRATION | narration_cfg

    try:
        tts = TTSFactory.get_provider()
    except Exception:
        raise HTTPException(status_code=503, detail="TTS provider unavailable")

    language = cfg["language"]
    test_text = NARRATION_TEST_TEXTS.get(language, NARRATION_TEST_TEXTS["en"])

    try:
        tts_result = await tts.synthesize(
            test_text,
            language=language,
            speed=cfg["speed"] * 10,
            pitch=cfg["pitch"],
            loudness=cfg["volume"],
        )
    except Exception:
        raise HTTPException(status_code=500, detail="TTS synthesis failed")

    audio = tts_result.audio
    return Response(content=audio, media_type="audio/mpeg")

