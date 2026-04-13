import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from app.services.tts.audio_pipeline import NarrationResult, SynthesisResult, encode_hls, synthesize


@pytest.mark.asyncio
async def test_synthesize_returns_none_for_empty_text():
    mock_tts = AsyncMock()
    result = await synthesize("", mock_tts, "en", 50, 50, 50)
    assert result is None


@pytest.mark.asyncio
async def test_synthesize_returns_none_for_punctuation_only():
    mock_tts = AsyncMock()
    result = await synthesize("...", mock_tts, "en", 50, 50, 50)
    assert result is None


@pytest.mark.asyncio
async def test_synthesize_builds_transcript_with_cumulative_timestamps():
    mock_tts = AsyncMock()
    mock_tts.synthesize.side_effect = [b"mp3_1", b"mp3_2"]

    with patch("app.services.tts.audio_pipeline._get_mp3_duration", side_effect=[2.0, 3.0]):
        result = await synthesize("Hello. World.", mock_tts, "en", 50, 50, 50)

    assert result is not None
    assert isinstance(result, SynthesisResult)
    assert result.transcript == [
        {"text": "Hello", "start": 0.0, "end": 2.0},
        {"text": "World", "start": 2.0, "end": 5.0},
    ]
    assert result.mp3_bytes == b"mp3_1" + b"mp3_2"


@pytest.mark.asyncio
async def test_synthesize_skips_sentence_when_tts_fails():
    mock_tts = AsyncMock()
    mock_tts.synthesize.side_effect = [Exception("TTS down"), b"mp3_2"]

    with patch("app.services.tts.audio_pipeline._get_mp3_duration", side_effect=[2.0]):
        result = await synthesize("Hello. World.", mock_tts, "en", 50, 50, 50)

    assert result is not None
    assert len(result.transcript) == 1
    assert result.transcript[0]["text"] == "World"
    assert result.mp3_bytes == b"mp3_2"


@pytest.mark.asyncio
async def test_synthesize_returns_none_when_all_sentences_fail():
    mock_tts = AsyncMock()
    mock_tts.synthesize.side_effect = Exception("TTS down")

    result = await synthesize("Hello. World.", mock_tts, "en", 50, 50, 50)
    assert result is None


@pytest.mark.asyncio
async def test_encode_hls_writes_mp3_and_calls_ffmpeg(tmp_path):
    synthesis = SynthesisResult(
        transcript=[{"text": "Hello", "start": 0.0, "end": 2.0}],
        mp3_bytes=b"fake_mp3_data",
    )

    mock_proc = AsyncMock()
    mock_proc.wait.return_value = 0

    with patch("asyncio.create_subprocess_exec", return_value=mock_proc) as mock_exec, \
         patch("app.services.tts.audio_pipeline.audio_store.register") as mock_register, \
         patch("tempfile.mkdtemp", return_value=str(tmp_path)):
        result = await encode_hls(synthesis)

    assert isinstance(result, NarrationResult)
    assert result.narration_id

    ffmpeg_args = mock_exec.call_args[0]
    assert ffmpeg_args[0] == "ffmpeg"
    assert "-i" in ffmpeg_args

    assert not (tmp_path / "input.mp3").exists()
    mock_register.assert_called_once_with(result.narration_id, tmp_path)


@pytest.mark.asyncio
async def test_encode_hls_raises_on_ffmpeg_failure(tmp_path):
    synthesis = SynthesisResult(
        transcript=[{"text": "Hello", "start": 0.0, "end": 2.0}],
        mp3_bytes=b"fake_mp3_data",
    )

    mock_proc = AsyncMock()
    mock_proc.wait.return_value = 1

    with patch("asyncio.create_subprocess_exec", return_value=mock_proc), \
         patch("tempfile.mkdtemp", return_value=str(tmp_path)):
        with pytest.raises(RuntimeError, match="ffmpeg exited"):
            await encode_hls(synthesis)
