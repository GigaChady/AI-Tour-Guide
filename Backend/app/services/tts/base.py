from abc import ABC, abstractmethod


class TTSProvider(ABC):
    @abstractmethod
    async def synthesize(self, text: str, language: str, speed: int, pitch: int, loudness: int) -> bytes:
        pass