from app.services.tts.base import TTSProvider
#TODO implement Google TTS provider. For now, it just raises NotImplementedError, and users should set TTS_PROVIDER=edge in config to use the Edge TTS provider instead.

class GoogleTTSProvider(TTSProvider):
    async def synthesize(self, text: str, language: str, speed: int, pitch: int, loudness: int) -> bytes:
        raise NotImplementedError("Google TTS provider not yet implemented. Set TTS_PROVIDER=edge.")
