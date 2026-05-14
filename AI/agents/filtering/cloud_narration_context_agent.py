import logging

from configs.cloud_narration_config import CloudFilteringSettings
from prompts.filtering_prompt_builder import FilteringPromptBuilder
from schemas import EnrichedPoi, NarrationSettings


logger = logging.getLogger(__name__)


class CloudNarrationContextAgent:
    """
    Builds narration context for the cloud narrative model.
    """

    def __init__(
        self,
        narration_settings: NarrationSettings,
        cloud_filtering_config: CloudFilteringSettings | None = None,
        prompt_builder: FilteringPromptBuilder | None = None,
    ):
        self.narration_settings = narration_settings
        self.prompt_builder = prompt_builder or FilteringPromptBuilder()
        self.cloud_filtering_config = (
            cloud_filtering_config
            or getattr(narration_settings, "cloud_filtering", None)
            or CloudFilteringSettings()
        )
        logger.info(
            "CloudNarrationContextAgent initialized with prompt_language: %s",
            getattr(self.cloud_filtering_config, "prompt_language", "en"),
        )

    def build_context(
        self,
        poi_name: str,
        poi_description: str | None = None,
        user_preferences: str | None = None,
    ) -> str:
        return self.prompt_builder.build_cloud_prompt(
            poi_name=poi_name,
            poi_description=poi_description,
            user_preferences=user_preferences,
            include_prompt=self.cloud_filtering_config.include_prompt,
        )

    def filter_information(self, enriched_poi: EnrichedPoi) -> str:
        return self.build_context(
            poi_name=enriched_poi.poi.name,
            poi_description=enriched_poi.to_context_text(),
        )
