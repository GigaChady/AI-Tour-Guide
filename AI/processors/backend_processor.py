import logging
import json


from processors.abstract_processor import AbstractProcessor
from narration.mocks.mocks import _mock_pois, _mock_narration
from pydantic import ValidationError
from utils.schemas import LocationEvent, PoisMessage, NarrationMessage, PreferencesEvent

logger = logging.getLogger(__name__)


class BackendProcessor(AbstractProcessor):

    def _start_narration_pipeline(self, event: LocationEvent, preferences: PreferencesEvent):
        """
        Starts the narration pipeline for the given location event and user preferences.
        """

        # Validate Narration Settings
        narration_settings = self.sub_processor.validate(event, preferences)

        # Generate Narration
        narration = self.sub_processor.process(event.session_id, narration_settings)

        return narration



    def process(self, event: LocationEvent, preferences: PreferencesEvent) -> tuple[str, NarrationMessage, PoisMessage]:
        """
        Returns session_id, narration_message, pois_message
        """
        if not isinstance(event, LocationEvent) or not isinstance(preferences, PreferencesEvent):
            logger.warning("Invalid event or preferences type: %s, %s", type(event), type(preferences))
            raise ValueError("Expected LocationEvent and PreferencesEvent instances")

        logger.debug(f"IS_MOCK: {self.is_mock}")
        if self.is_mock:
            logger.info("Processing mock stream for session %s", event.session_id)
            pois = _mock_pois(event.session_id, event.lat, event.lng)
            narration = _mock_narration(event.session_id, preferences, event.lat, event.lng, pois)
        else:
            logger.debug("Processing narration stream for session %s", event.session_id)
            narration = self._start_narration_pipeline(event, preferences)
            pois = _mock_pois(event.session_id, event.lat, event.lng)
            narration = _mock_narration(event.session_id, preferences, event.lat, event.lng, pois, narration)

            pass

        logger.info("Processed stream for session %s", event.session_id)
        return event.session_id, narration.model_dump_json(), PoisMessage(data=pois).model_dump_json()

    def validate(self, entry_id, payload, **kwargs):
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
            prefs_event = PreferencesEvent.model_validate(prefs)
        except ValidationError as exc:
            logger.warning("Invalid preferences cache: %s", prefs)
            raise ValueError(f"Invalid preferences cache") from exc

        return prefs_event