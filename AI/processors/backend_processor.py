import logging
import json
import os


from pydantic import ValidationError
from processors.contracts import NarrationService
from schemas import LocationEvent, PoisMessage, NarrationMessage, PreferencesEvent

logger = logging.getLogger(__name__)


def _model_to_json(model) -> str:
    if hasattr(model, "model_dump"):
        return json.dumps(model.model_dump(), ensure_ascii=False, separators=(",", ":"))
    return model.json(ensure_ascii=False)


class BackendProcessor:
    def __init__(self, narration_processor: NarrationService, is_mock: bool | None = None):
        self.narration_processor = narration_processor
        self.is_mock = (
            is_mock
            if is_mock is not None
            else os.getenv("AI_MOCK", False).lower() in ("true", "1", "t")
        )

    def _start_narration_pipeline(self, event: LocationEvent, preferences: PreferencesEvent):
        """
        Starts the narration pipeline for the given location event and user preferences.
        """

        # Validate Narration Settings
        narration_settings = self.narration_processor.validate(event, preferences)

        # Generate Narration
        narration_msg, poi_msg = self.narration_processor.process(event.session_id, narration_settings)

        return narration_msg, poi_msg



    def process(self, event: LocationEvent, preferences: PreferencesEvent) -> tuple[str, NarrationMessage, PoisMessage]:
        """
        Returns session_id, narration_message, pois_message
        """
        if not isinstance(event, LocationEvent) or not isinstance(preferences, PreferencesEvent):
            logger.warning("Invalid event or preferences type: %s, %s", type(event), type(preferences))
            raise ValueError("Expected LocationEvent and PreferencesEvent instances")

        if self.is_mock:
            from mocks.responses import mock_narration, mock_pois

            logger.info("Processing mock stream for session %s", event.session_id)
            poi = mock_pois(event.session_id, event.lat, event.lng)
            narration = mock_narration(event.session_id, preferences, event.lat, event.lng, poi)
            return (
                event.session_id,
                _model_to_json(narration),
                _model_to_json(PoisMessage(data=poi)),
            )
        else:
            logger.debug("Processing narration stream for session %s", event.session_id)
            narration, poi = self._start_narration_pipeline(event, preferences)
            if not poi:
                logger.warning("Failed to start narration pipeline for session %s", event.session_id)
                return event.session_id, None, None
            if not narration:
                return event.session_id, None, _model_to_json(poi)
            return event.session_id, _model_to_json(narration), _model_to_json(poi)

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

