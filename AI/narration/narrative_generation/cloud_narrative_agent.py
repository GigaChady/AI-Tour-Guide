import logging

from langchain_core.prompts import ChatPromptTemplate
from langchain_nvidia_ai_endpoints import ChatNVIDIA

from narration.configs.cloud_narration_config import CloudNarrativeSettings
from narration.narrative_generation.abstract_narrative_generation_agent import (
    AbstractNarrativeGenerationAgent,
)
from parsers.narration_response_parser import NarrationResponseParser
from prompts.narration_prompt_builder import NarrationPromptBuilder
from utils.schemas import NarrationSettings


class CloudNarrativeAgent(AbstractNarrativeGenerationAgent):
    def __init__(
        self,
        narration_settings: NarrationSettings,
        cloud_narrative_config: CloudNarrativeSettings | None = None,
        model_name: str | None = None,
        prompt_builder: NarrationPromptBuilder | None = None,
        response_parser: NarrationResponseParser | None = None,
    ):
        super().__init__(narration_settings)
        self.prompt_builder = prompt_builder or NarrationPromptBuilder()
        self.response_parser = response_parser or NarrationResponseParser()

        self.cloud_narrative_config = (
            cloud_narrative_config
            or getattr(narration_settings, "cloud_narrative", None)
            or CloudNarrativeSettings()
        )

        selected_model_name = model_name or self.cloud_narrative_config.model_name

        self.model = ChatNVIDIA(
            model=selected_model_name,
            temperature=self.cloud_narrative_config.temperature,
            top_p=self.cloud_narrative_config.top_p,
            max_tokens=self.cloud_narrative_config.max_tokens,
        )

    def generate_narration(self, location_name: str, location_info: str):
        logging.info("Starting narration generation about: %s", location_name)

        prompt_template = ChatPromptTemplate.from_messages(
            self.prompt_builder.build_messages(
                location_name=location_name,
                location_info=location_info,
                user_preferences=self.narration_settings.user_preferences,
                language_name=self.narration_settings.language.language_name,
            )
        )

        chain = prompt_template | self.model
        response = chain.invoke({})

        logging.info("Finished narration generation")
        return self.response_parser.parse(response.content)
