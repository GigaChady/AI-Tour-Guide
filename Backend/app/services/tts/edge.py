import edge_tts

from app.services.tts.base import TTSProvider

_VOICE_MAP: dict[str, str] = {
    "pl": "pl-PL-ZofiaNeural",
    "en": "en-US-JennyNeural",
    "es": "es-ES-ElviraNeural",
    "fr": "fr-FR-DeniseNeural",
    "de": "de-DE-KatjaNeural",
    "it": "it-IT-ElsaNeural",
    "pt": "pt-BR-FranciscaNeural",
    "ru": "ru-RU-SvetlanaNeural",
    "uk": "uk-UA-PolinaNeural",
    "ja": "ja-JP-NanamiNeural",
}

_DEFAULT_VOICE = "en-US-JennyNeural"


def _voice_for_language(language: str) -> str:
    return _VOICE_MAP.get(language, _DEFAULT_VOICE)


def _map_rate(value: int) -> str:
    """Map 0-100 to -50% to +50% rate (50 = +0% normal)."""
    pct = value - 50
    sign = "+" if pct >= 0 else ""
    return f"{sign}{pct}%"


def _map_pitch(value: int) -> str:
    """Map 0-100 to -50Hz to +50Hz (50 = +0Hz neutral)."""
    hz = value - 50
    sign = "+" if hz >= 0 else ""
    return f"{sign}{hz}Hz"


def _map_volume(value: int) -> str:
    """Map 0-100 to -50% to +50% volume (50 = +0% neutral)."""
    pct = value - 50
    sign = "+" if pct >= 0 else ""
    return f"{sign}{pct}%"


class EdgeTTSProvider(TTSProvider):
    async def synthesize(self, text: str, language: str, speed: int, pitch: int, loudness: int) -> bytes:
        voice = _voice_for_language(language)
        communicate = edge_tts.Communicate(
            text,
            voice,
            rate=_map_rate(speed),
            pitch=_map_pitch(pitch),
            volume=_map_volume(loudness),
        )
        audio = b""
        async for chunk in communicate.stream():
            if chunk["type"] == "audio":
                audio += chunk["data"]
        return audio
