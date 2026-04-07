from app.services.tts.base import TTSProvider


class EdgeTTSProvider(TTSProvider):
    async def synthesize(self, text: str, language: str, speed: int, pitch: int, loudness: int) -> bytes:
        raise NotImplementedError
