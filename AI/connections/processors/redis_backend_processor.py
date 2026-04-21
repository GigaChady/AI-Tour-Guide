import logging
import json


from connections.processors.abstract_processor import AbstractProcessor
from narration.mocks.mocks import _mock_pois, _mock_narration
from pydantic import ValidationError
from utils.schemas import LocationEvent, PoisMessage, NarrationMessage, PreferencesEvent

logger = logging.getLogger(__name__)


class RedisBackendProcessor(AbstractProcessor):

    def process(self, event: LocationEvent, preferences: PreferencesEvent) -> tuple[str, NarrationMessage, PoisMessage]:
        """
        Returns session_id, narration_message, pois_message
        """
        if not isinstance(event, LocationEvent) or not isinstance(preferences, PreferencesEvent):
            logger.warning("Invalid event or preferences type: %s, %s", type(event), type(preferences))
            raise ValueError("Expected LocationEvent and PreferencesEvent instances")

        pois = _mock_pois(event.session_id, event.lat, event.lng)
        narration = _mock_narration(event.session_id, preferences, event.lat, event.lng, pois)
        logger.info("Processed stream for session %s", event.session_id)
        return event.session_id, narration.model_dump_json(), PoisMessage(data=pois).model_dump_json()

    def validate(self, entry_id, payload):
        """
        Validates and return session_id, LocationEvent
        """
        try:
            event = LocationEvent.model_validate(payload)
        except ValidationError as exc:
            logger.warning("Invalid stream event %s: %s", entry_id, payload)
            raise ValueError(f"Invalid backend stream payload for entry {entry_id}") from exc


        return event.session_id, event

    def validate_prefs(self, prefs):
        """
        Validates Preferences and return PreferencesEvent
        """
        if not prefs:
            logger.warning("No preferences found")

        try:
            # skip validation for now
            # TODO: Validate against a schema once we have a better idea of what preferences look like
            prefs_event = PreferencesEvent(data=prefs)
        except ValidationError as exc:
            logger.warning("Invalid preferences cache: %s", prefs)
            raise ValueError(f"Invalid preferences cache") from exc

        return prefs_event