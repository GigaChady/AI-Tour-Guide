from abc import ABC, abstractmethod
from dataclasses import dataclass, field


@dataclass
class TTSResult:
    audio: bytes
    words: list[dict] = field(default_factory=list)



class TTSProvider(ABC):
    @abstractmethod
    async def synthesize(self, text: str, language: str, speed: int, pitch: int, loudness: int) -> TTSResult:
        pass