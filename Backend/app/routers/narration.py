from fastapi import APIRouter, Depends

from app.core.celery_app import celery_app
from app.core.dependencies import get_current_user
from app.models.models import User
from app.schemas.schemas import NarrationGenerateRequest, NarrationStatusResponse

router = APIRouter(prefix="/narration", tags=["narration"])

_STATE_MAP = {
    "PENDING": "pending",
    "STARTED": "processing",
    "SUCCESS": "done",
    "FAILURE": "failed",
}


@router.post("/generate")
async def generate_narration(
    body: NarrationGenerateRequest,
    current_user: User = Depends(get_current_user),
):
    task = celery_app.send_task(
        "workers.narrative_worker.narrate",
        kwargs={
            "text": body.text,
            "language": body.language,
            "speed": body.speed,
            "pitch": body.pitch,
            "loudness": body.loudness,
        },
        queue="narrative",
    )
    return {"task_id": task.id}


@router.get("/{task_id}", response_model=NarrationStatusResponse)
async def get_narration_status(
    task_id: str,
    current_user: User = Depends(get_current_user),
):
    result = celery_app.AsyncResult(task_id)
    status = _STATE_MAP.get(result.state, "pending")
    audio_url = (
        result.result.get("audio_url")
        if result.state == "SUCCESS" and isinstance(result.result, dict)
        else None
    )
    return NarrationStatusResponse(status=status, audio_url=audio_url)
