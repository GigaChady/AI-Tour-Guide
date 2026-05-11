
from narration.narration_manager import NarrationManager
from processors.abstract_processor import AbstractProcessor
from utils.schemas import NarrationSettings, NarrationDetailLevel, NarrationLanguage, LocationEvent, PreferencesEvent, \
    NarrationMessage, Poi, PoisMessage
from narration.configs.narration_configs import NarrationSettings as NarrationSettingsConfig
import logging

logger = logging.getLogger(__name__)

class NarrationProcessor(AbstractProcessor):

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
        if not isinstance(location, LocationEvent) or not isinstance(prefs, PreferencesEvent):
            raise ValueError("Expected LocationEvent and PreferencesEvent instances")

        # Load default configuration
        defaults = NarrationSettingsConfig()
        
        # Extract fields from preferencesEvent with fallback to defaults
        # PreferencesEvent has extra="allow", so custom fields can be passed
        detail_level_value = getattr(
            prefs, 
            'detail_level', 
            defaults.default_detail_level.value
        )
        
        search_radius = getattr(
            prefs, 
            'search_radius', 
            defaults.default_search_radius
        )
        
        language_tag = getattr(
            prefs, 
            'language', 
            defaults.default_language
        )
        
        language_name = getattr(
            prefs, 
            'language_name', 
            defaults.default_language_name
        )
        
        user_preferences = getattr(
            prefs, 
            'user_preferences', 
            ", ".join(prefs.interests) if prefs.interests else ""
        )

        raw_is_narration = getattr(location, 'is_narration', None)
        # `None` means "not provided"; keep narration enabled by default.
        is_narration = True if raw_is_narration is None else bool(raw_is_narration)

        raw_photo_count = getattr(location, 'include_photos', None)
        photo_count = 2 if raw_photo_count is None else int(raw_photo_count)
        # Guarantee at least one photo in response.
        photo_count = max(1, photo_count)
        
        # Build and return NarrationSettings from schemas
        nar_settings = NarrationSettings(
            latitude=location.lat,
            longitude=location.lng,
            detail_level=NarrationDetailLevel(detail_level_value),
            search_radius=search_radius,
            language=NarrationLanguage(
                language_name=language_name,
                language_tag=language_tag
            ),
            user_preferences=user_preferences,
            ollama_base_url=defaults.ollama_base_url,
            include_narration=is_narration,
            photo_count=photo_count
        )

        logger.info("Narration settings validated and created: %s", nar_settings)
        
        return nar_settings

    def _generate_photo(self, category, count):
        """
        Generates narration photo for a given point of interest (POI).
        """
        if self.sub_processor:
            return self.sub_processor.generate(category, count)


    def _generate_narration_response(self, narration):
        """
        Generates Narration Response (as defined in schemas) from narration text and list of POIs.
        """
        
        # TODO: Refactor code with the use of schemas to avoid raw json conversion
        
        narration_text = narration['narration']
        location = narration['location']
        
        output_text = f"{narration_text}"
        
        response = NarrationMessage(text=output_text)
        
        return response
    
    def _generate_poi_response(self, poi, location_raw_information, photo_count=1):
        """
        Generates POI Response (as defined in schemas) from poi data and raw location information.
        
        Maps poi dict (from OpenStreetMap) and raw location info (from scraping) to Poi schema.
        """
        # TODO: Refactor code with the use of schemas to avoid raw json conversion

        photo_urls = []
        if photo_count > 0:
            for i in range(photo_count):
                photo_urls.append(self._generate_photo(poi["category"], i))


        logger.info("Generated photo URLs for POI '%s': %s", poi["name"], photo_urls)

        poi_response = Poi(
            name=poi["name"],
            photos=photo_urls,
            desc=poi["name"],  # Raw text from scraping
            lat=poi["lat"],
            lng=poi["lon"]
        )

        poi_msg = PoisMessage(data=[poi_response])
        
        return poi_msg



    def process(self, session_id, narration_settings: NarrationSettings, **kwargs):
        """
        Runs narration pipeline
        """
        narration_manager = NarrationManager.build_narration_manager(narration_settings)

        try:
            narration, poi, location_raw_information = narration_manager.get_narration()
        except Exception as e:
            logger.error("Failed to get narration from Narration Manager: %s", e)
            return None, None

        logger.info("LLM-processing successfully generated for session %s", session_id)

        if narration:
            return self._generate_narration_response(narration), self._generate_poi_response(poi, location_raw_information, narration_settings.photo_count)

        return None, self._generate_poi_response(
            poi,
            location_raw_information,
            narration_settings.photo_count,
        )

    def validate_prefs(self, prefs):
        pass


    def generate(self, *args):
        pass