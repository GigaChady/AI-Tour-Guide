import json
import logging

from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama

from utils.schemas import NarrationSettings
from narration.narrative_generation.abstract_narrative_generation_agent import AbstractNarrativeGenerationAgent


class OllamaNarrativeGenerationAgent(AbstractNarrativeGenerationAgent):
    def __init__(self, narration_settings: NarrationSettings, model_name="mistral-nemo"):
        super().__init__(narration_settings)
        self.model = ChatOllama(
            model=model_name,
            temperature=0.4,
            format="json",
            base_url=narration_settings.ollama_base_url
        )
        logging.info("OllamaNarrativeGenerationAgent initialized with model: %s", model_name)

    def generate_narration(self, location_name: str, location_info: str):
        logging.info(f"Starting narration generation about: {location_name}")
        system_prompt = (
            "Jesteś przewodnikiem turystycznym. Twoim zadaniem jest stworzenie narracji o podanym miejscu. \n"
            "ZASADY:\n"
            "1. Użyj dostarczonych faktów, ale opowiedz je w sposób ciekawy.\n"
            "2. Dostosuj narrację do preferencji użytkownika.\n"
            "3. Zwróć wynik wyłącznie jako poprawny JSON z polami: 'title', 'narration'"
            f"4. Narracja i tytuł mają być w następującym języku: {self.narration_settings.language.language_name}"
            "5. W narracji wypisz wszystko w jednej linii, nie używaj znaków łamania linii"
        )

        user_prompt = (
            f"MIEJSCE: {location_name}\n"
            f"PREFERENCJE UŻYTKOWNIKA: {self.narration_settings.user_preferences}\n"
            f"FAKTY: {location_info}"
        )

        prompt_template = ChatPromptTemplate.from_messages([
            ("system", system_prompt),
            ("user", user_prompt)
        ])

        chain = prompt_template | self.model
        response = chain.invoke({})
        logging.info("Finished narration generation")
        try:
            return json.loads(response.content)
        except json.JSONDecodeError as e:
            logging.error(f"Invalid narration response. JSON decoding error: {e}")