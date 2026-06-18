"""Cloud narration configuration settings."""

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class CloudFilteringSettings(BaseSettings):
	"""
	Configuration settings for cloud filtering prompt composition.

	This class keeps cloud filtering lightweight: it does not configure an LLM,
	only the prompt composition details used by CloudFilteringAgent.
	"""

	model_config = SettingsConfigDict(
		env_prefix="CLOUD_FILTERING_",
		case_sensitive=False,
		extra="ignore",
	)

	include_prompt: bool = Field(
		default=False,
		description="Whether to include the prompt fo narration generation",
	)


class CloudNarrativeSettings(BaseSettings):
	"""
	Configuration settings for the cloud narrative generation agent.

	This class mirrors the existing Ollama-based narration settings, but keeps
	the configuration focused on the CloudNarrativeAgent model parameters.
	"""

	model_config = SettingsConfigDict(
		env_prefix="CLOUD_NARRATIVE_",
		case_sensitive=False,
		extra="ignore",
	)

	model_name: str = Field(
		default="gemini-flash-lite-latest",
		description="Cloud model name used for narration generation",
	)

	thinking_level: str = Field(
		default="HIGH",
		description="Gemini thinking level for narration generation (e.g. HIGH, LOW)",
	)

	temperature: float = Field(
		default=0.3,
		ge=0.0,
		le=1.0,
		description="Sampling temperature for cloud narration generation",
	)

	top_p: float = Field(
		default=0.9,
		ge=0.0,
		le=1.0,
		description="Top-p nucleus sampling for cloud narration generation",
	)

	max_tokens: int = Field(
		default=1024,
		ge=1,
		description="Maximum number of output tokens for narration (also covers thinking budget)",
	)

	request_timeout_seconds: int = Field(
		default=45,
		ge=1,
		description="Timeout for a single cloud narration generation request",
	)

	max_retries: int = Field(
		default=2,
		ge=0,
		description="Number of retries after failed or timed out cloud narration requests",
	)

	retry_backoff_seconds: float = Field(
		default=2.0,
		ge=0.0,
		description="Base delay between cloud narration retries; multiplied by attempt number",
	)


