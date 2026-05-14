import logging

from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama

from configs.narration_configs import FilteringOllamaSettings
from prompts.filtering_prompt_builder import FilteringPromptBuilder
from schemas import EnrichedPoi, NarrationSettings


class FilteringAgent:
    def __init__(
        self,
        narration_settings: NarrationSettings,
        filtering_config: FilteringOllamaSettings = None,
        prompt_builder: FilteringPromptBuilder | None = None,
    ):
        self.narration_settings = narration_settings
        self.prompt_builder = prompt_builder or FilteringPromptBuilder()

        config = (
            filtering_config
            or narration_settings.filtering_ollama
            if hasattr(narration_settings, "filtering_ollama")
            else FilteringOllamaSettings()
        )

        self.model = ChatOllama(
            model=config.model_name,
            temperature=config.temperature,
            top_k=config.top_k,
            top_p=config.top_p,
            num_predict=config.num_predict,
            base_url=narration_settings.ollama_base_url,
        )
        logging.info(
            "FilteringAgent initialized with model: %s, temperature: %s, top_k: %s, top_p: %s, num_predict: %s",
            config.model_name,
            config.temperature,
            config.top_k,
            config.top_p,
            config.num_predict,
        )

    def filter_information(self, enriched_poi: EnrichedPoi):
        logging.info("Starting information filtering...")

        prompt_template = ChatPromptTemplate.from_messages(
            self.prompt_builder.build_messages(
                poi_name=enriched_poi.poi.name,
                raw_text=enriched_poi.to_context_text(),
                user_preferences=self.narration_settings.user_preferences,
            )
        )

        chain = prompt_template | self.model
        response = chain.invoke({})

        logging.info("Finished information filtering")
        return response.content
