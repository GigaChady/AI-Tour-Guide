from app.services.tts.base import TTSProvider


class GoogleTTSProvider(TTSProvider):
    async def synthesize(self, text: str, language: str, speed: int, pitch: int, loudness: int) -> bytes:
        raise NotImplementedError("Google TTS provider not yet implemented. Set TTS_PROVIDER=edge.")
