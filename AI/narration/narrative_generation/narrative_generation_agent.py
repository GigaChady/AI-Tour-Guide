import logging

from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama

from narration.configs.narration_configs import NarrativeGenerationOllamaSettings
from narration.narrative_generation.abstract_narrative_generation_agent import (
    AbstractNarrativeGenerationAgent,
)
from parsers.narration_response_parser import NarrationResponseParser
from prompts.narration_prompt_builder import NarrationPromptBuilder
from utils.schemas import NarrationSettings


class OllamaNarrativeGenerationAgent(AbstractNarrativeGenerationAgent):
    def __init__(
        self,
        narration_settings: NarrationSettings,
        narrative_config: NarrativeGenerationOllamaSettings = None,
        prompt_builder: NarrationPromptBuilder | None = None,
        response_parser: NarrationResponseParser | None = None,
    ):
        super().__init__(narration_settings)
        self.prompt_builder = prompt_builder or NarrationPromptBuilder()
        self.response_parser = response_parser or NarrationResponseParser()

        config = (
            narrative_config
            or narration_settings.narrative_generation_ollama
            if hasattr(narration_settings, "narrative_generation_ollama")
            else NarrativeGenerationOllamaSettings()
        )

        self.model = ChatOllama(
            model=config.model_name,
            temperature=config.temperature,
            format=config.format,
            top_k=config.top_k,
            top_p=config.top_p,
            num_predict=config.num_predict,
            repeat_penalty=config.repeat_penalty,
            base_url=narration_settings.ollama_base_url,
        )
        logging.info(
            "OllamaNarrativeGenerationAgent initialized with model: %s, temperature: %s, format: %s, top_k: %s, top_p: %s, num_predict: %s, repeat_penalty: %s",
            config.model_name,
            config.temperature,
            config.format,
            config.top_k,
            config.top_p,
            config.num_predict,
            config.repeat_penalty,
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
