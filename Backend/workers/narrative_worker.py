import asyncio
import os
from app.core.config import settings
from app.core.celery_app import celery_app
from app.services.tts.factory import TTSFactory


@celery_app.task(name="workers.narrative_worker.narrate", bind=True)
def narrate(
    self,
    text: str,
    language: str,
    speed: int,
    pitch: int,
    loudness: int,
) -> dict:
    os.makedirs(settings.AUDIO_DIR, exist_ok=True)
    provider = TTSFactory.get_provider()
    audio_bytes = asyncio.run(
        provider.synthesize(text, language, speed=speed, pitch=pitch, loudness=loudness)
    )
    filename = f"{self.request.id}.mp3"
    filepath = os.path.join(settings.AUDIO_DIR, filename)
    with open(filepath, "wb") as f:
        f.write(audio_bytes)
    return {"audio_url": f"/audio/{filename}"}


