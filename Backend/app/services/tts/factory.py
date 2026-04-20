from app.services.tts.base import TTSProvider


class TTSFactory:
    @staticmethod
    def get_provider() -> TTSProvider:
        from app.core.config import settings  

        if settings.TTS_PROVIDER == "edge":
            from app.services.tts.edge import EdgeTTSProvider
            return EdgeTTSProvider()
        if settings.TTS_PROVIDER == "google":
            from app.services.tts.google import GoogleTTSProvider
            return GoogleTTSProvider()
        raise ValueError(f"Unknown TTS provider: {settings.TTS_PROVIDER}")
