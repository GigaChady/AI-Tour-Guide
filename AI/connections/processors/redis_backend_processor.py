import logging
import json


from connections.processors.abstract_processor import AbstractProcessor
from narration.mocks.mocks import _mock_pois, _mock_narration
from pydantic import ValidationError
from utils.schemas import BackendLocationEvent, PoisMessage

logger = logging.getLogger(__name__)


class RedisBackendProcessor(AbstractProcessor):

    def process(self, client, entry_id, payload, pubsub_prefix) -> None:
        try:
            event = BackendLocationEvent.model_validate(payload)
        except ValidationError as exc:
            logger.warning("Invalid stream event %s: %s", entry_id, payload)
            raise ValueError(f"Invalid backend stream payload for entry {entry_id}") from exc

        pois = _mock_pois(event.session_id, event.lat, event.lng)
        narration = _mock_narration(event.session_id, event.lat, event.lng, pois)
        logger.info("Processed stream event %s for session %s", entry_id, event.session_id)
        self.publish(client, event.session_id, pois, narration, pubsub_prefix=pubsub_prefix)

    def publish(self, client, session_id, pois, narration, pubsub_prefix) -> None:
        channel = f"{pubsub_prefix}{session_id}"
        client.publish(channel, json.dumps(PoisMessage(data=pois).model_dump(), ensure_ascii=False))
        client.publish(channel, json.dumps(narration.model_dump(), ensure_ascii=False))
        logger.info("Published stream event for session %s", session_id)
