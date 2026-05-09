import json
import logging

from langchain_core.prompts import ChatPromptTemplate
from langchain_nvidia_ai_endpoints import ChatNVIDIA
from unidecode import unidecode

from narration.configs.cloud_narration_config import CloudNarrativeSettings
from utils.schemas import NarrationSettings
from narration.narrative_generation.abstract_narrative_generation_agent import AbstractNarrativeGenerationAgent


class CloudNarrativeAgent(AbstractNarrativeGenerationAgent):
    def __init__(
        self,
        narration_settings: NarrationSettings,
        cloud_narrative_config: CloudNarrativeSettings | None = None,
        model_name: str | None = None,
    ):
        super().__init__(narration_settings)

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

        system_prompt = (
            "You are a tourist guide. Your task is to create a narration about the given place.\n"
            "RULES:\n"
            "1. Use the provided facts, but present them in an interesting and engaging way.\n"
            "2. Adapt the narration to the user's preferences.\n"
            "3. Return the result only as valid JSON with the field: 'narration'.\n"
            f"4. The narration must be written in the following language: {self.narration_settings.language.language_name}.\n"
            "5. Write the narration as a single line. Do not use newline characters.\n"
            "6. Do not add markdown, comments, explanations, or any text outside the JSON."
        )

        user_prompt = (
            f"LOCATION: {location_name}\n"
            f"USER's PREFERENCES: {self.narration_settings.user_preferences}\n"
            f"FACTS: {location_info}"
        )

        prompt_template = ChatPromptTemplate.from_messages([
            ("system", system_prompt),
            ("user", user_prompt),
        ])

        chain = prompt_template | self.model
        response = chain.invoke({})

        logging.info("Finished narration generation")

        try:
            result = json.loads(response.content)

            if isinstance(result, dict):
                for key, value in result.items():
                    if isinstance(value, str):
                        result[key] = unidecode(value)

            logging.info("Narration with ASCII conversion: %s", result)
            return result

        except json.JSONDecodeError as e:
            logging.error("Invalid narration response. JSON decoding error: %s", e)
            logging.error("Raw response: %s", response.content)
            return unidecode(response.content)