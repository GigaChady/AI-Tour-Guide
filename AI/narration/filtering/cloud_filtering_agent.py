import logging

from narration.configs.cloud_narration_config import CloudFilteringSettings
from narration.filtering.abstract_filtering_agent import AbstractFilteringAgent
from prompts.filtering_prompt_builder import FilteringPromptBuilder
from utils.schemas import NarrationSettings


logger = logging.getLogger(__name__)


class CloudFilteringAgent(AbstractFilteringAgent):
    """
    Builds a filtering prompt for cloud narration generation.
    """

    def __init__(
        self,
        narration_settings: NarrationSettings,
        cloud_filtering_config: CloudFilteringSettings | None = None,
        prompt_builder: FilteringPromptBuilder | None = None,
    ):
        super().__init__(narration_settings)
        self.prompt_builder = prompt_builder or FilteringPromptBuilder()
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
        return self.prompt_builder.build_cloud_prompt(
            poi_name=poi_name,
            poi_description=poi_description,
            user_preferences=user_preferences or self.narration_settings.user_preferences,
            include_prompt=self.cloud_filtering_config.include_prompt,
        )

    def filter_information(self, poi_name, raw_text):
        return self.build_prompt(
            poi_name=poi_name,
            poi_description=raw_text,
            user_preferences=self.narration_settings.user_preferences,
        )
