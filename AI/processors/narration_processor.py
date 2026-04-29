from narration.narration_manager import NarrationManager
from processors.abstract_processor import AbstractProcessor
from utils.schemas import NarrationSettings, NarrationDetailLevel, NarrationLanguage, LocationEvent, PreferencesEvent
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
            ollama_base_url=defaults.ollama_base_url
        )

        logger.info("Narration settings validated and created: %s", nar_settings)
        
        return nar_settings

    def process(self, session_id, narration_settings: NarrationSettings, **kwargs):
        """
        Runs narration pipeline
        """
        narration_manager = NarrationManager.build_narration_manager(narration_settings)

        try:
            narration = narration_manager.get_narration()
        except Exception as e:
            logger.error("Failed to get narration from Narration Manager: %s", e)
            raise Exception("Failed to get narration from Narration Manager")

        logger.info("Narration successfully generated for session %s", session_id)

        return narration


    def validate_prefs(self, prefs):
        pass