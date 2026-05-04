import logging

from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama

from narration.filtering.abstract_filtering_agent import AbstractFilteringAgent
from narration.configs.narration_configs import FilteringOllamaSettings
from utils.schemas import NarrationSettings


class FilteringAgent(AbstractFilteringAgent):
    def __init__(self, narration_settings: NarrationSettings, filtering_config: FilteringOllamaSettings = None):
        super().__init__(narration_settings)
        
        # Use provided config or get from narration_settings
        config = filtering_config or narration_settings.filtering_ollama if hasattr(narration_settings, 'filtering_ollama') else FilteringOllamaSettings()
        
        self.model = ChatOllama(
            model=config.model_name,
            temperature=config.temperature,
            top_k=config.top_k,
            top_p=config.top_p,
            num_predict=config.num_predict,
            base_url=narration_settings.ollama_base_url
        )
        logging.info("FilteringAgent initialized with model: %s, temperature: %s, top_k: %s, top_p: %s, num_predict: %s", 
                    config.model_name, config.temperature, config.top_k, config.top_p, config.num_predict)

    def filter_information(self, poi_name, raw_text):
        logging.info("Starting information filtering...")
        system_prompt = (
            "Twoim zadaniem jest wyciągnięcie z tekstu wyłącznie istotnych faktów i informacji o podanym miejscu.\n"
            "ZASADY:\n"
            "1. Usuń informacje o cenach biletów, godzinach otwarcia i linkach.\n"
            "2. Jeśli podano preferencje, szukaj informacji, które do nich pasują.\n"
            "3. Zwróć listę punktową w języku polskim."
        )

        user_prompt = (
            f"MIEJSCE: {poi_name}\n"
            f"PREFERENCJE: {self.narration_settings.user_preferences}\n"
            f"TEKST:\n{raw_text}")

        prompt_template = ChatPromptTemplate.from_messages([
            ("system", system_prompt),
            ("user", user_prompt)
        ])

        chain = prompt_template | self.model
        response = chain.invoke({})

        logging.info("Finished information filtering")
        return response.content
