from app.services.tts.base import TTSProvider

_provider_override: str | None = None


def set_tts_provider(provider: str) -> None:
    global _provider_override
    _provider_override = provider


class TTSFactory:
    @staticmethod
    def get_provider() -> TTSProvider:
        from app.core.config import settings

        provider = _provider_override or settings.TTS_PROVIDER

        if provider == "edge":
            from app.services.tts.edge import EdgeTTSProvider
            return EdgeTTSProvider()
        if provider == "google":
            from app.services.tts.google import GoogleTTSProvider
            return GoogleTTSProvider()
        raise ValueError(f"Unknown TTS provider: {provider}")
