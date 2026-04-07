from celery import Celery
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
    task_acks_late=True,                # potwierdzenie po wykonaniu, nie po odebraniu
    task_reject_on_worker_lost=True,    # retry gdy worker niespodziewanie padnie
    task_routes={  # pozwala rozdzielić zadania do różnych kolejek i workerów
        "workers.scraping_worker.scrape":       {"queue": "scraping"},
        "workers.filtering_worker.filter_pois": {"queue": "filtering"},
        "workers.narrative_worker.narrate":     {"queue": "narrative"},
    },
)

from celery.schedules import crontab

celery_app.conf.beat_schedule = {
    "cleanup-audio-files-hourly": {
        "task": "workers.cleanup_worker.cleanup_audio",
        "schedule": crontab(minute=0),  # top of every hour
    },
}

# @celery_app.task
# def example_task(x, y):
#     return x + y
