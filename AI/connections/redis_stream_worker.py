from __future__ import annotations
import logging
import time
import redis
import json
from connections.configs.redis_config import RedisWorkerConfig
from connections.processors.abstract_processor import AbstractProcessor
from connections.processors.backend_processor import BackendProcessor

logger = logging.getLogger(__name__)

def _build_client(redis_url: str) -> redis.Redis:
    return redis.from_url(redis_url, decode_responses=True)


class RedisStreamWorker:
    def __init__(self, config: RedisWorkerConfig, processor: AbstractProcessor, client: redis.Redis | None = None):
        self.config = config
        self.processor = processor
        self.client = client or _build_client(config.redis_url)
        self.last_id = config.start_id

    def _read_batch(self):
        response = self.client.xread(
            {self.config.stream_key: self.last_id},
            count=self.config.count,
            block=self.config.block_ms,
        )
        if not response:
            return []

        entries = []
        for _, stream_entries in response:
            entries.extend(stream_entries)
        return entries

    def _publish(self, session_id, poi_message, narration_message) -> None:
        channel = f"{self.config.pubsub_prefix}{session_id}"
        self.client.publish(channel, poi_message)
        self.client.publish(channel, narration_message)
        logger.info("Published stream event for session %s", session_id)


    def run(self) -> None:
        logger.info("Starting AI stream worker for %s", self.config.stream_key)
        while True:
            try:
                entries = self._read_batch()
                if not entries:
                    continue

                for entry_id, payload in entries:
                    try:
                        # Validate correctness of the message
                        session_id, event = self.processor.validate(entry_id, payload)

                        # Get Cached preferences for the session, if any
                        prefs_json = self.client.get(f"{self.config.pref_cache}{session_id}")
                        prefs = json.loads(prefs_json) if prefs_json else {}
                        logger.info("Validated stream event %s for session %s", entry_id, session_id)

                        prefs_event = self.processor.validate_prefs(prefs)
                        logger.info("Validated preferences event for session %s", session_id)

                        # Process into narration and pois messages
                        session_id, narration_msg, pois_msg = self.processor.process(event, prefs_event)

                        self._publish(session_id, pois_msg, narration_msg)

                    except Exception:
                        logger.exception("Failed to process stream event %s; skipping", entry_id)
                    finally:
                        # Always move stream cursor forward to avoid poison-message loops.
                        self.last_id = entry_id
            except KeyboardInterrupt:
                logger.info("Stream worker stopped by user")
                return
            except Exception:
                logger.exception("Unhandled error in stream worker; retrying in 2s")
                time.sleep(2)


def run_worker() -> None:
    config = RedisWorkerConfig()
    worker = RedisStreamWorker(config=config, processor=BackendProcessor())
    worker.run()


