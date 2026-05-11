import logging

from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama

from narration.configs.narration_configs import FilteringOllamaSettings
from narration.filtering.abstract_filtering_agent import AbstractFilteringAgent
from prompts.filtering_prompt_builder import FilteringPromptBuilder
from utils.schemas import NarrationSettings


class FilteringAgent(AbstractFilteringAgent):
    def __init__(
        self,
        narration_settings: NarrationSettings,
        filtering_config: FilteringOllamaSettings = None,
        prompt_builder: FilteringPromptBuilder | None = None,
    ):
        super().__init__(narration_settings)
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

    def filter_information(self, poi_name, raw_text):
        logging.info("Starting information filtering...")

        prompt_template = ChatPromptTemplate.from_messages(
            self.prompt_builder.build_messages(
                poi_name=poi_name,
                raw_text=raw_text,
                user_preferences=self.narration_settings.user_preferences,
            )
        )

        chain = prompt_template | self.model
        response = chain.invoke({})

        logging.info("Finished information filtering")
        return response.content
