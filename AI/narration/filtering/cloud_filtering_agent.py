import logging

from narration.configs.cloud_narration_config import CloudFilteringSettings
from narration.filtering.abstract_filtering_agent import AbstractFilteringAgent
from utils.schemas import NarrationSettings


logger = logging.getLogger(__name__)


class CloudFilteringAgent(AbstractFilteringAgent):
    """
    Class that builds a prompt with proper formating in point list for external llm with preferences.
    """
    def __init__(
        self,
        narration_settings: NarrationSettings,
        cloud_filtering_config: CloudFilteringSettings | None = None,
    ):
        super().__init__(narration_settings)
        self.cloud_filtering_config = (
            cloud_filtering_config
            or getattr(narration_settings, "cloud_filtering", None)
            or CloudFilteringSettings()
        )
        logger.info(
            "CloudFilteringAgent initialized with prompt_language: %s",
            getattr(self.cloud_filtering_config, "prompt_language", "en"),
        )

    def build_prompt(
        self,
        poi_name: str,
        poi_description: str | None = None,
        user_preferences: str | None = None,
    ) -> str:
        description = (poi_description or "").strip() or "No POI description provided."
        preferences = (
            (user_preferences or self.narration_settings.user_preferences or "").strip()
            or "No user preferences provided."
        )

        if self.cloud_filtering_config.include_prompt is False:
            return f"User preferences: {preferences}"

        return (
            "You are preparing a cloud filtering prompt for a travel narration pipeline.\n"
            f"POI name: {poi_name.strip()}\n"
            f"POI description: {description}\n"
            f"User preferences: {preferences}\n"
            "Compose a concise English prompt that keeps only the most relevant factual details for this POI."
        )

    def filter_information(self, poi_name, raw_text):
        return self.build_prompt(
            poi_name=poi_name,
            poi_description=raw_text,
            user_preferences=self.narration_settings.user_preferences,
        )
