import asyncio
import io
import shutil
import tempfile
import uuid
from dataclasses import dataclass
from pathlib import Path

from mutagen.mp3 import MP3

from app.core.audio_store import audio_store
from app.services.tts.base import TTSProvider
from app.services.tts.chunker import split as chunk_text


@dataclass
class SynthesisResult:
    transcript: list[dict]  # [{text, start, end}]
    mp3_bytes: bytes


@dataclass
class NarrationResult:
    narration_id: str
    hls_dir: Path


def _get_mp3_duration(mp3_bytes: bytes) -> float:
    bio = io.BytesIO(mp3_bytes)
    return MP3(bio).info.length


async def _synthesize_chunk(
    tts: TTSProvider,
    sentence: str,
    language: str,
    speed: int,
    pitch: int,
    loudness: int,
) -> bytes | None:
    try:
        return await tts.synthesize(sentence, language=language, speed=speed, pitch=pitch, loudness=loudness)
    except Exception:
        return None


async def synthesize(
    text: str,
    tts: TTSProvider,
    language: str,
    speed: int,
    pitch: int,
    loudness: int,
) -> SynthesisResult | None:
    chunks = chunk_text(text)
    if not chunks:
        return None

    results = await asyncio.gather(*[
        _synthesize_chunk(tts, sentence, language, speed, pitch, loudness)
        for _, sentence in chunks
    ])

    transcript: list[dict] = []
    mp3_parts: list[bytes] = []
    cursor = 0.0

    for (_chunk_id, sentence), mp3 in zip(chunks, results):
        if mp3 is None:
            continue
        duration = _get_mp3_duration(mp3)
        transcript.append({"text": sentence, "start": round(cursor, 3), "end": round(cursor + duration, 3)})
        mp3_parts.append(mp3)
        cursor += duration

    if not mp3_parts:
        return None

    return SynthesisResult(transcript=transcript, mp3_bytes=b"".join(mp3_parts))


async def encode_hls(synthesis: SynthesisResult) -> NarrationResult:
    narration_id = str(uuid.uuid4())
    hls_dir = Path(tempfile.mkdtemp())
    input_path = hls_dir / "input.mp3"

    input_path.write_bytes(synthesis.mp3_bytes)

    try:
        proc = await asyncio.create_subprocess_exec(
            "ffmpeg", "-y",
            "-i", str(input_path),
            "-c:a", "aac",
            "-b:a", "128k",
            "-hls_time", "4",
            "-hls_playlist_type", "vod",
            "-hls_segment_filename", str(hls_dir / "seg%03d.ts"),
            str(hls_dir / "index.m3u8"),
            stdout=asyncio.subprocess.DEVNULL,
            stderr=asyncio.subprocess.DEVNULL,
        )
        returncode = await proc.wait()
        if returncode != 0:
            shutil.rmtree(hls_dir, ignore_errors=True)
            raise RuntimeError(f"ffmpeg exited with code {returncode}")
    finally:
        input_path.unlink(missing_ok=True)

    audio_store.register(narration_id, hls_dir)
    return NarrationResult(narration_id=narration_id, hls_dir=hls_dir)
