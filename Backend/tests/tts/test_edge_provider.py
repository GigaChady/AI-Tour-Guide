import pytest
from unittest.mock import MagicMock, AsyncMock, patch
from app.services.tts.edge import EdgeTTSProvider, _map_rate, _map_pitch, _map_volume, _voice_for_language


# --- Parameter mapping tests ---

def test_map_rate_at_50_returns_zero_percent():
    assert _map_rate(50) == "+0%"

def test_map_rate_at_0_returns_minus_50_percent():
    assert _map_rate(0) == "-50%"

def test_map_rate_at_100_returns_plus_50_percent():
    assert _map_rate(100) == "+50%"

def test_map_pitch_at_50_returns_zero_hz():
    assert _map_pitch(50) == "+0Hz"

def test_map_pitch_at_0_returns_minus_50hz():
    assert _map_pitch(0) == "-50Hz"

def test_map_pitch_at_100_returns_plus_50hz():
    assert _map_pitch(100) == "+50Hz"

def test_map_volume_at_50_returns_zero_percent():
    assert _map_volume(50) == "+0%"

def test_map_volume_at_0_returns_minus_50_percent():
    assert _map_volume(0) == "-50%"

def test_map_volume_at_100_returns_plus_50_percent():
    assert _map_volume(100) == "+50%"


# --- Voice mapping tests ---

def test_voice_for_polish():
    assert _voice_for_language("pl") == "pl-PL-ZofiaNeural"

def test_voice_for_english():
    assert _voice_for_language("en") == "en-US-JennyNeural"

def test_voice_for_unknown_falls_back_to_english():
    assert _voice_for_language("xx") == "en-US-JennyNeural"


# --- synthesize() with mocked edge_tts.Communicate ---

@pytest.fixture
def mock_communicate(monkeypatch):
    async def fake_stream():
        yield {"type": "audio", "data": b"fake_audio_chunk_1"}
        yield {"type": "audio", "data": b"fake_audio_chunk_2"}
        yield {"type": "WordBoundary", "data": {}}  # non-audio chunk, should be ignored

    mock_cls = MagicMock()
    mock_cls.return_value.stream = fake_stream
    monkeypatch.setattr("app.services.tts.edge.edge_tts.Communicate", mock_cls)
    return mock_cls


@pytest.mark.asyncio
async def test_synthesize_returns_audio_bytes(mock_communicate):
    provider = EdgeTTSProvider()
    result = await provider.synthesize("Hello", "en", speed=50, pitch=50, loudness=50)
    assert result == b"fake_audio_chunk_1fake_audio_chunk_2"


@pytest.mark.asyncio
async def test_synthesize_passes_correct_voice_and_params(mock_communicate):
    provider = EdgeTTSProvider()
    await provider.synthesize("Cześć", "pl", speed=75, pitch=25, loudness=100)
    mock_communicate.assert_called_once_with(
        "Cześć",
        "pl-PL-ZofiaNeural",
        rate=_map_rate(75),
        pitch=_map_pitch(25),
        volume=_map_volume(100),
    )
