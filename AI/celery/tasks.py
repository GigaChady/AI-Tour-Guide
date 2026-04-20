from celery_app import celery_app
import redis
import json

r = redis.Redis(host="redis", port=6379, decode_responses=True)

@celery_app.task(
    name="ai_tasks.generate_narration",
    autoretry_for=(Exception,),
    retry_backoff=True,
    max_retries=3
)
def generate_narration(session_id, lat, lng):

    try:
        # TODO: Proper Logic
        narration = (f"MOCK for lat {lat}, lng {lng}: The Colosseum, also known as the Flavian Amphitheatre, "
                     "is an iconic symbol of ancient Rome. Built between 70-80 AD, "
                     "it was used for gladiatorial contests and public spectacles. "
                     "With a capacity of around 50,000 spectators, it remains one of the "
                     "greatest architectural and engineering feats of the Roman Empire.")

        # Send to backend
        r.publish(f"tour:{session_id}", json.dumps({
            "type": "narration",
            "data": narration
        }))

    except Exception as e:
        r.publish(f"tour:{session_id}", json.dumps({
            "type": "error",
            "data": str(e)
        }))
        raise