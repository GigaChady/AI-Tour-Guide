
from schemas import PoiCandidate
from pipeline.tour_narration_pipeline_factory import TourNarrationPipelineFactory
from processors.contracts import PhotoGenerator
from schemas import BackendMessageMapper
from schemas import NarrationSettings, LocationEvent, PreferencesEvent
import logging

logger = logging.getLogger(__name__)

class NarrationProcessor:
    def __init__(
        self,
        photo_processor: PhotoGenerator | None = None,
        message_mapper: BackendMessageMapper | None = None,
        pipeline_factory: TourNarrationPipelineFactory | None = None,
        redis_client=None,
    ):
        self.photo_processor = photo_processor
        self.message_mapper = message_mapper or BackendMessageMapper()
        self.pipeline_factory = pipeline_factory or TourNarrationPipelineFactory()
        self.redis_client = redis_client

    def validate(self, location: LocationEvent, prefs: PreferencesEvent, **kwargs) -> NarrationSettings:
        """
        Validates and returns narration settings based on the location event and user preferences.
        
        Parses LocationEvent and PreferencesEvent into NarrationSettings, using default values
        from NarrationDefaultsConfig when specific fields are not provided in the preferences.
        
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
        if self.photo_processor:
            return self.photo_processor.generate(category, count)
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
        is_planning = not narration_settings.include_narration
        pipeline = self.pipeline_factory.create(
            narration_settings,
            session_id=session_id if is_planning else None,
            redis_client=self.redis_client if is_planning else None,
        )

        try:
            result = pipeline.run()
        except Exception as e:
            logger.error("Failed to get narration from TourNarrationPipeline: %s", e)
            return None, None

        logger.info("LLM-processing successfully generated for session %s", session_id)

        if result.selected_pois:
            poi_list = [
                (s.poi, self._generate_photo_urls(s.poi, narration_settings.photo_count))
                for s in result.selected_pois
            ]
            return None, self.message_mapper.build_pois_message_many(poi_list)

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
