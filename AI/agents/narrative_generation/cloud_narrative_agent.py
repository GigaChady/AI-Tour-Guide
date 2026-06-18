import logging
import os

from langchain_core.prompts import ChatPromptTemplate
from langchain_google_genai import ChatGoogleGenerativeAI

from configs.cloud_narration_config import CloudNarrativeSettings
from parsers.narration_response_parser import NarrationResponseParser
from prompts.narration_prompt_builder import NarrationPromptBuilder
from schemas import NarrationSettings
from utils.retry import call_with_timeout_retry


class CloudNarrativeAgent:
    def __init__(
        self,
        narration_settings: NarrationSettings,
        cloud_narrative_config: CloudNarrativeSettings | None = None,
        model_name: str | None = None,
        prompt_builder: NarrationPromptBuilder | None = None,
        response_parser: NarrationResponseParser | None = None,
    ):
        self.narration_settings = narration_settings
        self.prompt_builder = prompt_builder or NarrationPromptBuilder()
        self.response_parser = response_parser or NarrationResponseParser()

        self.cloud_narrative_config = (
            cloud_narrative_config
            or getattr(narration_settings, "cloud_narrative", None)
            or CloudNarrativeSettings()
        )

        selected_model_name = model_name or self.cloud_narrative_config.model_name

        model_kwargs = dict(
            model=selected_model_name,
            temperature=self.cloud_narrative_config.temperature,
            top_p=self.cloud_narrative_config.top_p,
            max_output_tokens=self.cloud_narrative_config.max_tokens,
            google_api_key=os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY"),
        )

        thinking_level = self.cloud_narrative_config.thinking_level
        try:
            self.model = ChatGoogleGenerativeAI(
                thinking_level=thinking_level, **model_kwargs
            )
        except TypeError:
            # Older langchain-google-genai versions don't accept thinking_level as a
            # constructor arg; pass it through the underlying generation config instead.
            self.model = ChatGoogleGenerativeAI(
                model_kwargs={"thinking_level": thinking_level}, **model_kwargs
            )

        logging.info(
            "CloudNarrativeAgent initialized with model: %s, temperature: %s, "
            "top_p: %s, max_output_tokens: %s, thinking_level: %s",
            selected_model_name,
            self.cloud_narrative_config.temperature,
            self.cloud_narrative_config.top_p,
            self.cloud_narrative_config.max_tokens,
            thinking_level,
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
        response = call_with_timeout_retry(
            lambda: chain.invoke({}),
            timeout_seconds=self.cloud_narrative_config.request_timeout_seconds,
            max_retries=self.cloud_narrative_config.max_retries,
            backoff_seconds=self.cloud_narrative_config.retry_backoff_seconds,
            operation_name="Cloud narration generation",
        )

        logging.info("Finished narration generation")
        return self.response_parser.parse(response.content)
