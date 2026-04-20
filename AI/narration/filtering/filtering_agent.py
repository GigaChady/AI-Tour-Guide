import logging

from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama

from narration.filtering.abstract_filtering_agent import AbstractFilteringAgent
from utils.schemas import NarrationSettings


class FilteringAgent(AbstractFilteringAgent):
    def __init__(self, narration_settings: NarrationSettings, model_name="mistral-nemo"):
        super().__init__(narration_settings)
        self.model = ChatOllama(
            model=model_name,
            temperature=0.1,
            base_url=narration_settings.ollama_base_url
        )

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
