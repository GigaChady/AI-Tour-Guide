from __future__ import annotations
import logging
import time
import uuid
import redis
import json
from connections.configs.redis_config import RedisWorkerConfig
from processors.contracts import StreamProcessor


logger = logging.getLogger(__name__)


class RedisStreamWorker:
    def __init__(self, processor: StreamProcessor, client: redis.Redis | None = None):
        self.config = RedisWorkerConfig()
        self.processor = processor
        self.client = client or redis.from_url(
            self.config.redis_url,
            decode_responses=True,
            socket_timeout=self.config.block_ms / 1000 + 5,
        )
        self.last_id = self.config.start_id

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
        narration_id = str(uuid.uuid4())

        poi_data = json.loads(poi_message)
        poi_data["narration_id"] = narration_id
        self.client.publish(channel, json.dumps(poi_data))

        if narration_message:
            nar_data = json.loads(narration_message)
            nar_data["narration_id"] = narration_id
            self.client.publish(channel, json.dumps(nar_data))
        logger.info("Published stream event for session %s", session_id)

    def _publish_error(self, session_id):
        channel = f"{self.config.pubsub_prefix}{session_id}"
        logger.info("Error in generating narration and POI - None detected due to server failure or no POI detection %s", session_id)


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

                        if pois_msg:
                            self._publish(session_id, pois_msg, narration_msg)
                        else:
                            self._publish_error(session_id)

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




