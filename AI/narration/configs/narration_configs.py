"""
Narration configuration module using pydantic-settings.
"""

from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field

from utils.schemas import NarrationDetailLevel, NarrationLanguage


class NarrationSettings(BaseSettings):
    """
    Configuration settings for narration services.
    
    This class uses pydantic-settings to manage narration configuration
    with support for environment variables and default values.
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )

    # Default narration settings
    default_detail_level: NarrationDetailLevel = Field(
        default=NarrationDetailLevel.DETAILED,
        description="Default detail level for narrations"
    )

    default_search_radius: int = Field(
        default=1500,
        description="Default search radius in meters for finding POIs"
    )

    default_language: str = Field(
        default="en",
        description="Default language code for narrations"
    )

    default_language_name: str = Field(
        default="English",
        description="Default language name for narrations"
    )

    # API Configuration
    ollama_base_url: str = Field(
        default="http://localhost:11434",
        description="Base URL for Ollama API"
    )

    ollama_model: str = Field(
        default="mistral",
        description="Default Ollama model for text generation"
    )

    # Generation Configuration
    max_narration_length: int = Field(
        default=500,
        description="Maximum length of generated narration in characters"
    )

    narration_temperature: float = Field(
        default=0.7,
        ge=0.0,
        le=1.0,
        description="Temperature for narration generation (0.0 to 1.0)"
    )

    # Timeout Configuration
    generation_timeout: int = Field(
        default=30,
        description="Timeout for narration generation in seconds"
    )

    @property
    def default_language_config(self) -> NarrationLanguage:
        """
        Returns the default language configuration.
        
        Returns:
            NarrationLanguage: Default language configuration object
        """
        return NarrationLanguage(
            language_name=self.default_language_name,
            language_tag=self.default_language
        )
