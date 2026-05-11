
from domain.pipeline_models import PoiCandidate
from pipeline.tour_narration_pipeline import TourNarrationPipeline
from processors.abstract_processor import AbstractProcessor
from transport.backend_message_mapper import BackendMessageMapper
from utils.schemas import NarrationSettings, LocationEvent, PreferencesEvent
import logging

logger = logging.getLogger(__name__)

class NarrationProcessor(AbstractProcessor):
    def __init__(self, sub_processor=None, message_mapper: BackendMessageMapper | None = None):
        super().__init__(sub_processor=sub_processor)
        self.message_mapper = message_mapper or BackendMessageMapper()

    def validate(self, location: LocationEvent, prefs: PreferencesEvent, **kwargs) -> NarrationSettings:
        """
        Validates and returns narration settings based on the location event and user preferences.
        
        Parses LocationEvent and PreferencesEvent into NarrationSettings, using default values
        from NarrationSettingsConfig when specific fields are not provided in the preferences.
        
        Args:
            location: LocationEvent containing latitude and longitude
            prefs: PreferencesEvent containing optional configuration fields
            **kwargs: Additional optional arguments
            
        Returns:
            NarrationSettings: Configured narration settings with location and preferences data
            
        Raises:
            ValueError: If location or prefs are not the expected types
        """
        narration_settings = self.message_mapper.build_narration_settings(
            location=location,
            prefs=prefs,
        )

        logger.info("Narration settings validated and created: %s", narration_settings)
        
        return narration_settings

    def _generate_photo(self, category: str, count: int):
        """
        Generates narration photo for a given point of interest (POI).
        """
        if self.sub_processor:
            return self.sub_processor.generate(category, count)
        return None

    def _generate_photo_urls(self, poi: PoiCandidate, photo_count: int = 1) -> list[str]:
        photo_urls = []
        if photo_count > 0:
            for i in range(photo_count):
                photo_url = self._generate_photo(poi.category, i)
                if photo_url:
                    photo_urls.append(photo_url)

        logger.info("Generated photo URLs for POI '%s': %s", poi.name, photo_urls)
        return photo_urls

    def process(self, session_id, narration_settings: NarrationSettings, **kwargs):
        """
        Runs narration pipeline
        """
        pipeline = TourNarrationPipeline.build_default(narration_settings)

        try:
            result = pipeline.run()
        except Exception as e:
            logger.error("Failed to get narration from TourNarrationPipeline: %s", e)
            return None, None

        logger.info("LLM-processing successfully generated for session %s", session_id)

        if result.poi is None:
            return None, None

        photo_urls = self._generate_photo_urls(
            result.poi,
            narration_settings.photo_count,
        )
        poi_response = self.message_mapper.build_pois_message(result.poi, photo_urls)

        if result.narration:
            return self.message_mapper.build_narration_message(result.narration), poi_response

        return None, poi_response

    def validate_prefs(self, prefs):
        pass


    def generate(self, *args):
        pass
