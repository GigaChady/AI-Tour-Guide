from abc import ABC, abstractmethod


class TTSProvider(ABC):
    @abstractmethod
    async def synthesize(self, text: str, language: str, speed: int, pitch: int, loudness: int) -> bytes:
        """Synthesize speech from text with given parameters. Returns audio data as bytes."""
        pass