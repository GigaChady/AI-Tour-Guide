from dataclasses import dataclass
import os

DEFAULT_STREAM_KEY = os.getenv("AI_STREAM_KEY", "location:events")
DEFAULT_PUBSUB_PREFIX = os.getenv("AI_PUBSUB_PREFIX", "tour:")
DEFAULT_REDIS_URL = os.getenv("REDIS_URL", "redis://redis:6379/0")
DEFAULT_BLOCK_MS = int(os.getenv("AI_STREAM_BLOCK_MS", "5000"))
DEFAULT_COUNT = int(os.getenv("AI_STREAM_COUNT", "10"))
DEFAULT_PREF_CACHE = str(os.getenv("REDIS_PREF_CACHE", "preferences:"))
DEFAULT_POI_SEEN_PREFIX = str(os.getenv("POI_SEEN_PREFIX", "poi:seen:"))
DEFAULT_POI_SEEN_TTL = int(os.getenv("POI_SEEN_TTL", "7200"))


@dataclass(frozen=True) # immutable
class RedisWorkerConfig:

    stream_key: str = DEFAULT_STREAM_KEY
    pubsub_prefix: str = DEFAULT_PUBSUB_PREFIX
    redis_url: str = DEFAULT_REDIS_URL
    block_ms: int = DEFAULT_BLOCK_MS
    count: int = DEFAULT_COUNT
    pref_cache: str = DEFAULT_PREF_CACHE
    poi_seen_prefix: str = DEFAULT_POI_SEEN_PREFIX
    poi_seen_ttl: int = DEFAULT_POI_SEEN_TTL
    start_id: str = "$"
