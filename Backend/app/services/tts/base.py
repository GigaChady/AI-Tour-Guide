from abc import ABC, abstractmethod


class TTSProvider(ABC):
    @abstractmethod
    async def synthesize(self, text: str, language: str, speed: int, pitch: int, loudness: int) -> bytes:
        """Synthesize text to MP3 audio bytes.

        Args:
            text: Text to synthesize.
            language: BCP-47 language code, e.g. "en", "pl".
            speed: 0-100 (50 = normal speed).
            pitch: 0-100 (50 = neutral pitch).
            loudness: 0-100 (50 = 0 dB gain).

        Returns:
            MP3 audio as bytes.
        """
