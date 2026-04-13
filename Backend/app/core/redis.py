import redis.asyncio as redis 
from .config import settings

_redis_client: redis.Redis = None

async def init_redis():
    global _redis_client
    _redis_client = await redis.from_url(settings.REDIS_URL, decode_responses=True)
    try:
        await _redis_client.xgroup_create("location:events", "llm-workers", id="0", mkstream=True)
    except redis.ResponseError:
        pass  # group already exists
 
 
async def close_redis():
    if _redis_client:
        await _redis_client.aclose()
 
 
def get_redis() -> redis.Redis:
    if not _redis_client:
        raise RuntimeError("Redis client is not initialized.")
    return _redis_client
 


