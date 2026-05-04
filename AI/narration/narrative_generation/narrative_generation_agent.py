import json
import logging

from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama
from unidecode import unidecode

from narration.configs.narration_configs import NarrativeGenerationOllamaSettings
from utils.schemas import NarrationSettings
from narration.narrative_generation.abstract_narrative_generation_agent import AbstractNarrativeGenerationAgent


class OllamaNarrativeGenerationAgent(AbstractNarrativeGenerationAgent):
    def __init__(self, narration_settings: NarrationSettings, narrative_config: NarrativeGenerationOllamaSettings = None):
        super().__init__(narration_settings)
        
        # Use provided config or get from narration_settings
        config = narrative_config or narration_settings.narrative_generation_ollama if hasattr(narration_settings, 'narrative_generation_ollama') else NarrativeGenerationOllamaSettings()
        
        self.model = ChatOllama(
            model=config.model_name,
            temperature=config.temperature,
            format=config.format,
            top_k=config.top_k,
            top_p=config.top_p,
            num_predict=config.num_predict,
            repeat_penalty=config.repeat_penalty,
            base_url=narration_settings.ollama_base_url
        )
        logging.info("OllamaNarrativeGenerationAgent initialized with model: %s, temperature: %s, format: %s, top_k: %s, top_p: %s, num_predict: %s, repeat_penalty: %s", 
                    config.model_name, config.temperature, config.format, config.top_k, config.top_p, config.num_predict, config.repeat_penalty)

    def generate_narration(self, location_name: str, location_info: str):
        logging.info(f"Starting narration generation about: {location_name}")
        system_prompt = (
            "Jesteś przewodnikiem turystycznym. Twoim zadaniem jest stworzenie narracji o podanym miejscu. \n"
            "ZASADY:\n"
            "1. Użyj dostarczonych faktów, ale opowiedz je w sposób ciekawy.\n"
            "2. Dostosuj narrację do preferencji użytkownika.\n"
            "3. Zwróć wynik wyłącznie jako poprawny JSON z polami: 'location', 'narration'"
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
            result = json.loads(response.content)
            # Convert all string values to ASCII by removing diacritical marks
            if isinstance(result, dict):
                for key, value in result.items():
                    if isinstance(value, str):
                        result[key] = unidecode(value)
            logging.info("Narration with ASCII conversion: %s", result)
            return result
        except json.JSONDecodeError as e:
            logging.error(f"Invalid narration response. JSON decoding error: {e}")
