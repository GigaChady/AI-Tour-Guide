import asyncio
import os
import time

from celery import Celery
from celery.schedules import crontab

from app.core.config import settings

celery_app = Celery(
    "tour_workers",
    broker=settings.REDIS_URL,
    backend=settings.REDIS_URL,
)

celery_app.conf.update(
    task_serializer="json",
    result_serializer="json",
    accept_content=["json"],
    task_acks_late=True,
    task_reject_on_worker_lost=True,
    task_routes={
        "workers.scraping_worker.scrape":       {"queue": "scraping"},
        "workers.filtering_worker.filter_pois": {"queue": "filtering"},
        "workers.narrative_worker.narrate":     {"queue": "narrative"},
    },
)

celery_app.conf.beat_schedule = {
    "cleanup-audio-files-hourly": {
        "task": "workers.cleanup_worker.cleanup_audio",
        "schedule": crontab(minute=0),
    },
}


@celery_app.task(name="workers.narrative_worker.narrate", bind=True)
def narrate(self, text: str, language: str, speed: int, pitch: int, loudness: int) -> dict:
    from app.core.config import settings as _settings
    from app.services.tts.factory import TTSFactory

    os.makedirs(_settings.AUDIO_DIR, exist_ok=True)
    provider = TTSFactory.get_provider()
    audio_bytes = asyncio.run(
        provider.synthesize(text, language, speed=speed, pitch=pitch, loudness=loudness)
    )
    filename = f"{self.request.id}.mp3"
    filepath = os.path.join(_settings.AUDIO_DIR, filename)
    with open(filepath, "wb") as f:
        f.write(audio_bytes)
    return {"audio_url": f"/audio/{filename}"}


@celery_app.task(name="workers.cleanup_worker.cleanup_audio")
def cleanup_audio() -> dict:
    from app.core.config import settings as _settings

    audio_dir = _settings.AUDIO_DIR
    if not os.path.exists(audio_dir):
        return {"deleted": 0}

    now = time.time()
    deleted = 0
    for fname in os.listdir(audio_dir):
        if fname == ".gitkeep":
            continue
        fpath = os.path.join(audio_dir, fname)
        if os.path.isfile(fpath) and (now - os.path.getmtime(fpath)) > settings.AUDIO_FILE_TTL_SECONDS:
            os.remove(fpath)
            deleted += 1
    return {"deleted": deleted}
