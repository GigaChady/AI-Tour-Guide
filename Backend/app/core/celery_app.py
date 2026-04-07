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
        "workers.cleanup_worker.cleanup_audio": {"queue": "cleanup"},
    },
)

celery_app.conf.beat_schedule = {
    "cleanup-audio-files-hourly": {
        "task": "workers.cleanup_worker.cleanup_audio",
        "schedule": crontab(minute=0),
    },
}




