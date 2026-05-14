"""
Narration configuration module using pydantic-settings.

Configuration Environment Variables:
====================================

Filtering Ollama Settings (FILTERING_OLLAMA_ prefix):
    - FILTERING_OLLAMA_MODEL_NAME: Model name for filtering (default: mistral-nemo)
    - FILTERING_OLLAMA_TEMPERATURE: Temperature for filtering (default: 0.05)
    - FILTERING_OLLAMA_TOP_K: Top-k sampling (default: 20, lower = faster)
    - FILTERING_OLLAMA_TOP_P: Top-p nucleus sampling (default: 0.7, lower = faster)
    - FILTERING_OLLAMA_NUM_PREDICT: Max tokens to generate (default: 256, lower = faster)

Cloud Filtering Settings (CLOUD_FILTERING_ prefix):
    - CLOUD_FILTERING_PROMPT_LANGUAGE: Language used for the composed prompt (default: en)
    - CLOUD_FILTERING_INCLUDE_PROMPT: Whether to include the Prompt for narration (default: false)

Cloud Narrative Settings (CLOUD_NARRATIVE_ prefix):
    - CLOUD_NARRATIVE_MODEL_NAME: Model name for cloud narration generation (default: meta/llama-3.1-8b-instruct)
    - CLOUD_NARRATIVE_TEMPERATURE: Temperature for cloud narration generation (default: 0.4)
    - CLOUD_NARRATIVE_TOP_P: Top-p nucleus sampling (default: 0.9)
    - CLOUD_NARRATIVE_MAX_TOKENS: Max tokens to generate (default: 700)

Narrative Generation Ollama Settings (NARRATIVE_OLLAMA_ prefix):
    - NARRATIVE_OLLAMA_MODEL_NAME: Model name for narration generation (default: mistral-nemo)
    - NARRATIVE_OLLAMA_TEMPERATURE: Temperature for narration (default: 0.2)
    - NARRATIVE_OLLAMA_FORMAT: Output format (default: json)
    - NARRATIVE_OLLAMA_TOP_K: Top-k sampling (default: 30)
    - NARRATIVE_OLLAMA_TOP_P: Top-p nucleus sampling (default: 0.8)
    - NARRATIVE_OLLAMA_NUM_PREDICT: Max tokens to generate (default: 512)
    - NARRATIVE_OLLAMA_REPEAT_PENALTY: Penalty for repeating tokens (default: 1.1)

Example .env file for fast mode:
    OLLAMA_BASE_URL=http://ollama:11434
    FILTERING_OLLAMA_TEMPERATURE=0.05
    FILTERING_OLLAMA_TOP_K=15
    FILTERING_OLLAMA_TOP_P=0.6
    FILTERING_OLLAMA_NUM_PREDICT=200
    NARRATIVE_OLLAMA_MODEL_NAME=neural-chat-7b
    NARRATIVE_OLLAMA_TOP_K=20
    NARRATIVE_OLLAMA_TOP_P=0.7
    NARRATIVE_OLLAMA_NUM_PREDICT=400
"""
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field

from narration.configs.cloud_narration_config import CloudFilteringSettings, CloudNarrativeSettings
from utils.schemas import NarrationDetailLevel, NarrationLanguage


class FilteringOllamaSettings(BaseSettings):
    """
    Configuration settings for Ollama model used in filtering agent.
    Handles extraction and filtering of relevant information from raw text.
    
    Can be configured via environment variables with FILTERING_OLLAMA_ prefix.
    Example: FILTERING_OLLAMA_MODEL_NAME=mistral, FILTERING_OLLAMA_TEMPERATURE=0.1
    """
    model_config = SettingsConfigDict(
        env_prefix="FILTERING_OLLAMA_",
        case_sensitive=False,
        extra="ignore"
    )

    model_name: str = Field(
        default="mistral-nemo",
        description="Ollama model name for filtering"
    )

    temperature: float = Field(
        default=0.05,
        ge=0.0,
        le=1.0,
        description="Temperature for filtering (0.0 to 1.0, lower = more deterministic)"
    )

    top_k: int = Field(
        default=20,
        description="Top-k sampling parameter (lower = faster, more deterministic)"
    )

    top_p: float = Field(
        default=0.7,
        ge=0.0,
        le=1.0,
        description="Top-p (nucleus) sampling (lower = faster, more deterministic)"
    )

    num_predict: int = Field(
        default=256,
        description="Maximum tokens to generate (lower = faster)"
    )



class NarrativeGenerationOllamaSettings(BaseSettings):
    """
    Configuration settings for Ollama model used in narrative generation agent.
    Handles creative narration generation with structured JSON output.
    
    Can be configured via environment variables with NARRATIVE_OLLAMA_ prefix.
    Example: NARRATIVE_OLLAMA_MODEL_NAME=mistral, NARRATIVE_OLLAMA_TEMPERATURE=0.4
    """
    model_config = SettingsConfigDict(
        env_prefix="NARRATIVE_OLLAMA_",
        case_sensitive=False,
        extra="ignore"
    )

    model_name: str = Field(
        default= "mistral-nemo",
        description="Ollama model name for narrative generation"
    )

    temperature: float = Field(
        default=0.2,
        ge=0.0,
        le=1.0,
        description="Temperature for narrative generation (0.0 to 1.0)"
    )

    format: str = Field(
        default="json",
        description="Output format for narrative generation"
    )

    top_k: int = Field(
        default=30,
        description="Top-k sampling parameter"
    )

    top_p: float = Field(
        default=0.8,
        ge=0.0,
        le=1.0,
        description="Top-p (nucleus) sampling"
    )

    num_predict: int = Field(
        default=512,
        description="Maximum tokens to generate"
    )

    repeat_penalty: float = Field(
        default=1.1,
        ge=1.0,
        description="Penalty for repeating tokens"
    )


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
        default=2500,
        description="Default search radius in meters for finding POIs"
    )

    default_language: str = Field(
        default="pl",
        description="Default language code for narrations"
    )

    default_language_name: str = Field(
        default="Polish",
        description="Default language name for narrations"
    )

    # API Configuration
    ollama_base_url: str = Field(
        default="http://localhost:11434",
        description="Base URL for Ollama API"
    )

    filtering_ollama: FilteringOllamaSettings = Field(
        default_factory=FilteringOllamaSettings,
        description="Configuration settings for filtering Ollama model"
    )

    cloud_filtering: CloudFilteringSettings = Field(
        default_factory=CloudFilteringSettings,
        description="Configuration settings for cloud filtering prompt composition"
    )

    cloud_narrative: CloudNarrativeSettings = Field(
        default_factory=CloudNarrativeSettings,
        description="Configuration settings for cloud narrative generation model"
    )

    narrative_generation_ollama: NarrativeGenerationOllamaSettings = Field(
        default_factory=NarrativeGenerationOllamaSettings,
        description="Configuration settings for narrative generation Ollama model"
    )

    max_narration_length: int = Field(
        default=500,
        description="Maximum length of generated narration in characters"
    )


    # Timeout Configuration
    generation_timeout: int = Field(
        default=90,
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
