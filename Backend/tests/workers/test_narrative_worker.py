import os
import pytest
from unittest.mock import MagicMock, AsyncMock

from workers.narrative_worker import narrate


def test_narrate_saves_mp3_and_returns_url(tmp_path, monkeypatch):
    monkeypatch.setattr("workers.narrative_worker.settings.AUDIO_DIR", str(tmp_path))

    mock_provider = MagicMock()
    mock_provider.synthesize = AsyncMock(return_value=b"fake_mp3_bytes")
    monkeypatch.setattr(
        "app.services.tts.factory.TTSFactory.get_provider",
        lambda: mock_provider,
    )

    task = narrate
    task.push_request(id="abc-123")
    try:
        result = task.run(text="Hello", language="en", speed=50, pitch=50, loudness=50)
    finally:
        task.pop_request()

    assert result == {"audio_url": "/audio/abc-123.mp3"}
    assert (tmp_path / "abc-123.mp3").read_bytes() == b"fake_mp3_bytes"


def test_narrate_passes_params_to_provider(tmp_path, monkeypatch):
    monkeypatch.setattr("workers.narrative_worker.settings.AUDIO_DIR", str(tmp_path))

    mock_provider = MagicMock()
    mock_provider.synthesize = AsyncMock(return_value=b"data")
    monkeypatch.setattr(
        "app.services.tts.factory.TTSFactory.get_provider",
        lambda: mock_provider,
    )

    task = narrate
    task.push_request(id="xyz")
    try:
        task.run(text="Cześć", language="pl", speed=80, pitch=30, loudness=60)
    finally:
        task.pop_request()

    mock_provider.synthesize.assert_called_once_with(
        "Cześć", "pl", speed=80, pitch=30, loudness=60
    )
