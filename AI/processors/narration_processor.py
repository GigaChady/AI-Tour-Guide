from processors.abstract_processor import AbstractProcessor
from utils.schemas import NarrationSettings, NarrationDetailLevel, NarrationLanguage, LocationEvent, PreferencesEvent


class NarrationProcessor(AbstractProcessor):

    def validate(self, location: LocationEvent, prefs: PreferencesEvent, **kwargs):
        """
        Validates and returns narration settings based on the location event and user preferences.
        """
        if not isinstance(location, LocationEvent) or not isinstance(prefs, PreferencesEvent):
            raise ValueError("Expected LocationEvent and PreferencesEvent instances")

        nar_settings = NarrationSettings(
            detail_level=NarrationDetailLevel(prefs.detail_level),
            language=NarrationLanguage(prefs.language)
        )

    def process(self, entry_id, payload, **kwargs):
        """
        Runs narration pipeline
        """
        raise NotImplementedError("NarrationProcessor does not implement process method")