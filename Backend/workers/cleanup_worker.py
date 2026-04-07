import os
import time

from app.core.celery_app import celery_app
from app.core.config import settings

AUDIO_DIR = "audio_files"


@celery_app.task(name="workers.cleanup_worker.cleanup_audio")
def cleanup_audio() -> dict:
    if not os.path.exists(AUDIO_DIR):
        return {"deleted": 0}

    now = time.time()
    deleted = 0
    for fname in os.listdir(AUDIO_DIR):
        if fname == ".gitkeep":
            continue
        fpath = os.path.join(AUDIO_DIR, fname)
        if os.path.isfile(fpath) and (now - os.path.getmtime(fpath)) > settings.AUDIO_FILE_TTL_SECONDS:
            os.remove(fpath)
            deleted += 1
    return {"deleted": deleted}
