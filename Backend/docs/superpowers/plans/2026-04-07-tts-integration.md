# TTS Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate Coqui XTTS2 TTS into the narration pipeline — FastAPI endpoint fires a Celery task that synthesizes audio and returns a static file URL to the frontend.

**Architecture:** A `TTSProvider` abstraction with `CoquiTTSProvider` (active) and `GoogleTTSProvider` (stub) backed by a factory keyed on `TTS_PROVIDER` env var. The Celery `narrate` task calls the provider, saves the MP3 to `audio_files/`, and returns the URL. FastAPI mounts `/audio` as a static directory. A Celery beat `cleanup_audio` task deletes files older than `AUDIO_FILE_TTL_SECONDS`.

**Tech Stack:** Coqui `TTS` library (XTTS2 model), `pydub` (pitch/loudness post-processing), FastAPI `StaticFiles`, Celery (task + beat schedule), Redis (result backend).

---

## File Map

| Action | Path | Responsibility |
|---|---|---|
| Create | `app/services/tts/__init__.py` | Package init |
| Create | `app/services/tts/base.py` | Abstract `TTSProvider` |
| Create | `app/services/tts/factory.py` | Returns provider based on `TTS_PROVIDER` env var |
| Create | `app/services/tts/coqui.py` | XTTS2 implementation + param mapping |
| Create | `app/services/tts/google.py` | Google TTS stub (raises `NotImplementedError`) |
| Create | `workers/__init__.py` | Package init |
| Create | `workers/narrative_worker.py` | Celery `narrate` task: calls provider, saves MP3 |
| Create | `workers/cleanup_worker.py` | Celery beat task: deletes stale audio files |
| Create | `audio_files/.gitkeep` | Keeps the output directory in git |
| Create | `tests/tts/__init__.py` | Test package |
| Create | `tests/tts/test_tts_factory.py` | Factory unit tests |
| Create | `tests/tts/test_coqui_provider.py` | Coqui provider unit tests (model mocked) |
| Create | `tests/workers/__init__.py` | Test package |
| Create | `tests/workers/test_narrative_worker.py` | Narrative worker unit tests |
| Create | `tests/workers/test_cleanup_worker.py` | Cleanup worker unit tests |
| Create | `tests/narration/__init__.py` | Test package |
| Create | `tests/narration/test_narration_router.py` | Router integration tests |
| Modify | `app/core/config.py` | Add `TTS_PROVIDER`, `AUDIO_FILE_TTL_SECONDS`, `GOOGLE_APPLICATION_CREDENTIALS` |
| Modify | `app/core/celery_app.py` | Add beat schedule for `cleanup_audio` |
| Modify | `app/routers/narration.py` | Implement `/generate` and `/{task_id}` endpoints |
| Modify | `app/schemas/schemas.py` | Add `NarrationGenerateRequest`, `NarrationStatusResponse` |
| Modify | `app/main.py` | Mount `/audio` static directory |
| Modify | `requirements.txt` | Add `TTS`, `pydub` |
| Modify | `docker-compose.yml` | GPU passthrough + model cache volume |
| Modify | `.env.example` | Add new env vars |

---

## Pre-requisites

Install ffmpeg on your system (required by pydub for MP3 encoding):
- Windows: `choco install ffmpeg` or download from https://ffmpeg.org/download.html and add to PATH
- Verify: `ffmpeg -version`

---

## Task 1: Add dependencies and config

**Files:**
- Modify: `requirements.txt`
- Modify: `app/core/config.py`
- Modify: `.env.example`

- [ ] **Step 1: Add new packages to requirements.txt**

Add these two lines to `requirements.txt`:
```
TTS>=0.22.0
pydub>=0.25.1
```

- [ ] **Step 2: Add config fields to Settings**

In `app/core/config.py`, add inside the `Settings` class after the existing Redis fields:
```python
    # TTS
    TTS_PROVIDER: str = "coqui"                   # "coqui" or "google"
    TTS_SPEAKER_WAV: str = ""                      # optional: path to reference .wav for XTTS2 voice cloning
    AUDIO_FILE_TTL_SECONDS: int = 3600
    GOOGLE_APPLICATION_CREDENTIALS: str = ""       # only needed when TTS_PROVIDER=google
```

- [ ] **Step 3: Update .env.example**

Append to `.env.example`:
```
TTS_PROVIDER=coqui
TTS_SPEAKER_WAV=
AUDIO_FILE_TTL_SECONDS=3600
GOOGLE_APPLICATION_CREDENTIALS=
```

- [ ] **Step 4: Install new dependencies**

```bash
pip install TTS pydub
```

- [ ] **Step 5: Verify ffmpeg is available to pydub**

```bash
python -c "from pydub import AudioSegment; print('pydub ok')"
```
Expected: `pydub ok` (if ffmpeg is missing, you'll see a warning — install ffmpeg first)

- [ ] **Step 6: Commit**

```bash
git add requirements.txt app/core/config.py .env.example
git commit -m "feat: add TTS dependencies and config fields"
```

---

## Task 2: TTS provider abstraction

**Files:**
- Create: `app/services/tts/__init__.py`
- Create: `app/services/tts/base.py`
- Create: `app/services/tts/factory.py`
- Create: `tests/tts/__init__.py`
- Create: `tests/tts/test_tts_factory.py`

- [ ] **Step 1: Write failing factory tests**

Create `tests/tts/__init__.py` (empty).

Create `tests/tts/test_tts_factory.py`:
```python
import pytest
import os


def test_factory_returns_coqui_provider(monkeypatch):
    monkeypatch.setenv("TTS_PROVIDER", "coqui")
    # reload settings to pick up monkeypatched env
    import importlib
    import app.core.config as cfg_module
    importlib.reload(cfg_module)
    from app.services.tts.factory import TTSFactory
    from app.services.tts.coqui import CoquiTTSProvider
    provider = TTSFactory.get_provider()
    assert isinstance(provider, CoquiTTSProvider)


def test_factory_raises_for_unknown_provider(monkeypatch):
    monkeypatch.setenv("TTS_PROVIDER", "banana")
    import importlib
    import app.core.config as cfg_module
    importlib.reload(cfg_module)
    from app.services.tts.factory import TTSFactory
    with pytest.raises(ValueError, match="Unknown TTS provider: banana"):
        TTSFactory.get_provider()
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
pytest tests/tts/test_tts_factory.py -v
```
Expected: `ImportError` or `ModuleNotFoundError` — files don't exist yet

- [ ] **Step 3: Create the TTS package and base class**

Create `app/services/tts/__init__.py` (empty file).

Create `app/services/tts/base.py`:
```python
from abc import ABC, abstractmethod


class TTSProvider(ABC):
    @abstractmethod
    def synthesize(self, text: str, language: str, speed: int, pitch: int, loudness: int) -> bytes:
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
```

- [ ] **Step 4: Create the factory**

Create `app/services/tts/factory.py`:
```python
from app.services.tts.base import TTSProvider


class TTSFactory:
    @staticmethod
    def get_provider() -> TTSProvider:
        from app.core.config import settings  # late import so tests can reload config

        if settings.TTS_PROVIDER == "coqui":
            from app.services.tts.coqui import CoquiTTSProvider
            return CoquiTTSProvider()
        if settings.TTS_PROVIDER == "google":
            from app.services.tts.google import GoogleTTSProvider
            return GoogleTTSProvider()
        raise ValueError(f"Unknown TTS provider: {settings.TTS_PROVIDER}")
```

- [ ] **Step 5: Create placeholder provider files so imports resolve**

Create `app/services/tts/coqui.py` (temporary placeholder):
```python
from app.services.tts.base import TTSProvider


class CoquiTTSProvider(TTSProvider):
    def synthesize(self, text: str, language: str, speed: int, pitch: int, loudness: int) -> bytes:
        raise NotImplementedError
```

Create `app/services/tts/google.py`:
```python
from app.services.tts.base import TTSProvider


class GoogleTTSProvider(TTSProvider):
    def synthesize(self, text: str, language: str, speed: int, pitch: int, loudness: int) -> bytes:
        raise NotImplementedError("Google TTS provider not yet implemented. Set TTS_PROVIDER=coqui.")
```

- [ ] **Step 6: Run tests to verify they pass**

```bash
pytest tests/tts/test_tts_factory.py -v
```
Expected: 2 PASSED

- [ ] **Step 7: Commit**

```bash
git add app/services/tts/ tests/tts/
git commit -m "feat: add TTS provider abstraction and factory"
```

---

## Task 3: Coqui XTTS2 provider

**Files:**
- Modify: `app/services/tts/coqui.py` (replace placeholder)
- Create: `tests/tts/test_coqui_provider.py`

- [ ] **Step 1: Write failing Coqui provider tests**

Create `tests/tts/test_coqui_provider.py`:
```python
import io
import pytest
from unittest.mock import MagicMock, patch
from pydub import AudioSegment
from app.services.tts.coqui import CoquiTTSProvider, _map_speed, _map_pitch, _map_loudness


# --- Parameter mapping tests (pure functions, no mocking needed) ---

def test_map_speed_at_50_returns_1():
    assert _map_speed(50) == pytest.approx(1.0)


def test_map_speed_at_0_returns_0_5():
    assert _map_speed(0) == pytest.approx(0.5)


def test_map_speed_at_100_returns_1_5():
    assert _map_speed(100) == pytest.approx(1.5)


def test_map_pitch_at_50_returns_0():
    assert _map_pitch(50) == pytest.approx(0.0)


def test_map_pitch_at_0_returns_minus_6():
    assert _map_pitch(0) == pytest.approx(-6.0)


def test_map_pitch_at_100_returns_plus_6():
    assert _map_pitch(100) == pytest.approx(6.0)


def test_map_loudness_at_50_returns_0():
    assert _map_loudness(50) == pytest.approx(0.0)


def test_map_loudness_at_0_returns_minus_10():
    assert _map_loudness(0) == pytest.approx(-10.0)


def test_map_loudness_at_100_returns_plus_10():
    assert _map_loudness(100) == pytest.approx(10.0)


# --- synthesize() with mocked TTS model ---

@pytest.fixture
def mock_coqui_model(tmp_path):
    """Patches CoquiTTS so tts_to_file writes a real silent wav (no GPU needed)."""
    def fake_tts_to_file(text, file_path, **kwargs):
        silence = AudioSegment.silent(duration=100)  # 100ms silence
        silence.export(file_path, format="wav")

    mock_model = MagicMock()
    mock_model.tts_to_file.side_effect = fake_tts_to_file
    return mock_model


def test_synthesize_returns_mp3_bytes(mock_coqui_model, monkeypatch):
    monkeypatch.setattr(
        "app.services.tts.coqui.CoquiTTS",
        lambda *a, **kw: mock_coqui_model,
    )
    CoquiTTSProvider._model = None  # reset singleton
    provider = CoquiTTSProvider()
    result = provider.synthesize("Hello world", "en", speed=50, pitch=50, loudness=50)
    assert isinstance(result, bytes)
    assert len(result) > 0


def test_synthesize_passes_speed_to_model(mock_coqui_model, monkeypatch):
    monkeypatch.setattr(
        "app.services.tts.coqui.CoquiTTS",
        lambda *a, **kw: mock_coqui_model,
    )
    CoquiTTSProvider._model = None
    provider = CoquiTTSProvider()
    provider.synthesize("Test", "pl", speed=100, pitch=50, loudness=50)
    call_kwargs = mock_coqui_model.tts_to_file.call_args.kwargs
    assert call_kwargs["speed"] == pytest.approx(1.5)
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
pytest tests/tts/test_coqui_provider.py -v
```
Expected: multiple FAILED — `_map_speed`, `_map_pitch`, `_map_loudness` not defined yet

- [ ] **Step 3: Implement CoquiTTSProvider**

Replace `app/services/tts/coqui.py` entirely:
```python
import io
import os
import tempfile

from pydub import AudioSegment

from app.services.tts.base import TTSProvider

try:
    from TTS.api import TTS as CoquiTTS
except ImportError:  # pragma: no cover
    CoquiTTS = None  # type: ignore[assignment]

_DEFAULT_SPEAKER = "Claribel Dervla"


def _map_speed(value: int) -> float:
    """Map 0-100 to 0.5-1.5 (50 = 1.0 normal speed)."""
    return 0.5 + (value / 100.0)


def _map_pitch(value: int) -> float:
    """Map 0-100 to -6 to +6 semitones (50 = 0 neutral)."""
    return -6.0 + (value / 100.0) * 12.0


def _map_loudness(value: int) -> float:
    """Map 0-100 to -10 to +10 dB (50 = 0 neutral)."""
    return -10.0 + (value / 100.0) * 20.0


def _apply_pitch(audio: AudioSegment, semitones: float) -> AudioSegment:
    if semitones == 0:
        return audio
    new_rate = int(audio.frame_rate * (2.0 ** (semitones / 12.0)))
    return audio._spawn(audio.raw_data, overrides={"frame_rate": new_rate}).set_frame_rate(
        audio.frame_rate
    )


def _apply_loudness(audio: AudioSegment, db: float) -> AudioSegment:
    if db == 0:
        return audio
    return audio + db


class CoquiTTSProvider(TTSProvider):
    _model: "CoquiTTS | None" = None

    @classmethod
    def _get_model(cls) -> "CoquiTTS":
        if cls._model is None:
            if CoquiTTS is None:
                raise RuntimeError("TTS library is not installed. Run: pip install TTS")
            cls._model = CoquiTTS(
                "tts_models/multilingual/multi-dataset/xtts_v2",
                gpu=True,
            )
        return cls._model

    def synthesize(self, text: str, language: str, speed: int, pitch: int, loudness: int) -> bytes:
        from app.core.config import settings

        model = self._get_model()
        speaker_wav = settings.TTS_SPEAKER_WAV or None
        speaker = None if speaker_wav else _DEFAULT_SPEAKER

        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
            tmp_path = f.name

        try:
            model.tts_to_file(
                text=text,
                speaker_wav=speaker_wav,
                speaker=speaker,
                language=language,
                file_path=tmp_path,
                speed=_map_speed(speed),
            )
            audio = AudioSegment.from_wav(tmp_path)
            audio = _apply_pitch(audio, _map_pitch(pitch))
            audio = _apply_loudness(audio, _map_loudness(loudness))
            buf = io.BytesIO()
            audio.export(buf, format="mp3")
            return buf.getvalue()
        finally:
            os.unlink(tmp_path)
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
pytest tests/tts/test_coqui_provider.py -v
```
Expected: all PASSED

- [ ] **Step 5: Commit**

```bash
git add app/services/tts/coqui.py tests/tts/test_coqui_provider.py
git commit -m "feat: implement CoquiTTSProvider with XTTS2 and pydub post-processing"
```

---

## Task 4: Narration schemas

**Files:**
- Modify: `app/schemas/schemas.py`

No dedicated test task — schemas are validated implicitly by the router tests in Task 6.

- [ ] **Step 1: Add narration schemas to schemas.py**

In `app/schemas/schemas.py`, append after the existing `# ----------------LLM SCHEMAS----------------` section:
```python
class NarrationGenerateRequest(BaseModel):
    text: str
    language: str = "en"
    speed: int = 50    # 0-100, 50 = normal
    pitch: int = 50    # 0-100, 50 = neutral
    loudness: int = 50 # 0-100, 50 = 0 dB


class NarrationStatusResponse(BaseModel):
    status: Literal["pending", "processing", "done", "failed"]
    audio_url: str | None = None
```

- [ ] **Step 2: Commit**

```bash
git add app/schemas/schemas.py
git commit -m "feat: add NarrationGenerateRequest and NarrationStatusResponse schemas"
```

---

## Task 5: Narrative Celery worker

**Files:**
- Create: `workers/__init__.py`
- Create: `workers/narrative_worker.py`
- Create: `tests/workers/__init__.py`
- Create: `tests/workers/test_narrative_worker.py`

- [ ] **Step 1: Write failing narrative worker tests**

Create `tests/workers/__init__.py` (empty).

Create `tests/workers/test_narrative_worker.py`:
```python
import os
import pytest
from unittest.mock import MagicMock


def test_narrate_saves_mp3_and_returns_url(tmp_path, monkeypatch):
    monkeypatch.setattr("workers.narrative_worker.AUDIO_DIR", str(tmp_path))

    mock_provider = MagicMock()
    mock_provider.synthesize.return_value = b"fake_mp3_bytes"
    monkeypatch.setattr(
        "workers.narrative_worker.TTSFactory.get_provider",
        lambda: mock_provider,
    )

    from workers.narrative_worker import narrate

    mock_self = MagicMock()
    mock_self.request.id = "abc-123"

    result = narrate(mock_self, text="Hello", language="en", speed=50, pitch=50, loudness=50)

    assert result == {"audio_url": "/audio/abc-123.mp3"}
    assert (tmp_path / "abc-123.mp3").read_bytes() == b"fake_mp3_bytes"


def test_narrate_passes_params_to_provider(tmp_path, monkeypatch):
    monkeypatch.setattr("workers.narrative_worker.AUDIO_DIR", str(tmp_path))

    mock_provider = MagicMock()
    mock_provider.synthesize.return_value = b"data"
    monkeypatch.setattr(
        "workers.narrative_worker.TTSFactory.get_provider",
        lambda: mock_provider,
    )

    from workers.narrative_worker import narrate

    mock_self = MagicMock()
    mock_self.request.id = "xyz"

    narrate(mock_self, text="Cześć", language="pl", speed=80, pitch=30, loudness=60)

    mock_provider.synthesize.assert_called_once_with(
        "Cześć", "pl", speed=80, pitch=30, loudness=60
    )
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
pytest tests/workers/test_narrative_worker.py -v
```
Expected: `ModuleNotFoundError: No module named 'workers'`

- [ ] **Step 3: Create the workers package and narrative worker**

Create `workers/__init__.py` (empty).

Create `workers/narrative_worker.py`:
```python
import os

from app.core.celery_app import celery_app
from app.services.tts.factory import TTSFactory

AUDIO_DIR = "audio_files"


@celery_app.task(name="workers.narrative_worker.narrate", bind=True)
def narrate(
    self,
    text: str,
    language: str,
    speed: int,
    pitch: int,
    loudness: int,
) -> dict:
    os.makedirs(AUDIO_DIR, exist_ok=True)
    provider = TTSFactory.get_provider()
    audio_bytes = provider.synthesize(text, language, speed=speed, pitch=pitch, loudness=loudness)
    filename = f"{self.request.id}.mp3"
    filepath = os.path.join(AUDIO_DIR, filename)
    with open(filepath, "wb") as f:
        f.write(audio_bytes)
    return {"audio_url": f"/audio/{filename}"}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
pytest tests/workers/test_narrative_worker.py -v
```
Expected: 2 PASSED

- [ ] **Step 5: Commit**

```bash
git add workers/ tests/workers/
git commit -m "feat: add narrative Celery worker"
```

---

## Task 6: Narration router endpoints

**Files:**
- Modify: `app/routers/narration.py`
- Create: `tests/narration/__init__.py`
- Create: `tests/narration/test_narration_router.py`

- [ ] **Step 1: Write failing router tests**

Create `tests/narration/__init__.py` (empty).

Create `tests/narration/test_narration_router.py`:
```python
import pytest
from httpx import AsyncClient, ASGITransport
from unittest.mock import MagicMock, patch
from app.main import app


async def _get_auth_headers(client: AsyncClient) -> dict:
    """Register and login a test user, return Bearer headers."""
    await client.post("/auth/register", json={
        "email": "tts_test@example.com",
        "password": "Password123!",
    })
    resp = await client.post("/auth/login", json={
        "email": "tts_test@example.com",
        "password": "Password123!",
    })
    token = resp.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


@pytest.mark.asyncio
async def test_generate_narration_returns_task_id(monkeypatch):
    mock_task = MagicMock()
    mock_task.id = "task-uuid-001"
    monkeypatch.setattr(
        "app.routers.narration.celery_app.send_task",
        lambda *a, **kw: mock_task,
    )

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        headers = await _get_auth_headers(client)
        resp = await client.post("/narration/generate", json={
            "text": "Welcome to Warsaw!",
            "language": "pl",
            "speed": 50,
            "pitch": 50,
            "loudness": 50,
        }, headers=headers)

    assert resp.status_code == 200
    assert resp.json()["task_id"] == "task-uuid-001"


@pytest.mark.asyncio
async def test_get_narration_status_done(monkeypatch):
    mock_result = MagicMock()
    mock_result.state = "SUCCESS"
    mock_result.result = {"audio_url": "/audio/task-uuid-001.mp3"}
    monkeypatch.setattr(
        "app.routers.narration.celery_app.AsyncResult",
        lambda task_id: mock_result,
    )

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        headers = await _get_auth_headers(client)
        resp = await client.get("/narration/task-uuid-001", headers=headers)

    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "done"
    assert data["audio_url"] == "/audio/task-uuid-001.mp3"


@pytest.mark.asyncio
async def test_get_narration_status_pending(monkeypatch):
    mock_result = MagicMock()
    mock_result.state = "PENDING"
    mock_result.result = None
    monkeypatch.setattr(
        "app.routers.narration.celery_app.AsyncResult",
        lambda task_id: mock_result,
    )

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        headers = await _get_auth_headers(client)
        resp = await client.get("/narration/task-uuid-001", headers=headers)

    assert resp.status_code == 200
    assert resp.json()["status"] == "pending"
    assert resp.json()["audio_url"] is None


@pytest.mark.asyncio
async def test_generate_narration_requires_auth():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        resp = await client.post("/narration/generate", json={
            "text": "Hello",
            "language": "en",
            "speed": 50,
            "pitch": 50,
            "loudness": 50,
        })
    assert resp.status_code == 401
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
pytest tests/narration/test_narration_router.py -v
```
Expected: FAILED — endpoints return 422 or 404 (router is a stub)

- [ ] **Step 3: Implement the narration router**

Replace `app/routers/narration.py` entirely:
```python
from fastapi import APIRouter, Depends

from app.core.celery_app import celery_app
from app.core.dependencies import get_current_user
from app.models.models import User
from app.schemas.schemas import NarrationGenerateRequest, NarrationStatusResponse

router = APIRouter(prefix="/narration", tags=["narration"])

_STATE_MAP = {
    "PENDING": "pending",
    "STARTED": "processing",
    "SUCCESS": "done",
    "FAILURE": "failed",
}


@router.post("/generate")
async def generate_narration(
    body: NarrationGenerateRequest,
    current_user: User = Depends(get_current_user),
):
    task = celery_app.send_task(
        "workers.narrative_worker.narrate",
        kwargs={
            "text": body.text,
            "language": body.language,
            "speed": body.speed,
            "pitch": body.pitch,
            "loudness": body.loudness,
        },
        queue="narrative",
    )
    return {"task_id": task.id}


@router.get("/{task_id}", response_model=NarrationStatusResponse)
async def get_narration_status(
    task_id: str,
    current_user: User = Depends(get_current_user),
):
    result = celery_app.AsyncResult(task_id)
    status = _STATE_MAP.get(result.state, "pending")
    audio_url = (
        result.result.get("audio_url")
        if result.state == "SUCCESS" and isinstance(result.result, dict)
        else None
    )
    return NarrationStatusResponse(status=status, audio_url=audio_url)
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
pytest tests/narration/test_narration_router.py -v
```
Expected: 4 PASSED

- [ ] **Step 5: Commit**

```bash
git add app/routers/narration.py tests/narration/
git commit -m "feat: implement narration router with generate and status endpoints"
```

---

## Task 7: Cleanup Celery beat worker

**Files:**
- Create: `workers/cleanup_worker.py`
- Modify: `app/core/celery_app.py`
- Create: `tests/workers/test_cleanup_worker.py`

- [ ] **Step 1: Write failing cleanup worker tests**

Append to `tests/workers/test_cleanup_worker.py` (new file):
```python
import os
import time
import pytest


def _create_file(directory: str, name: str, age_seconds: float) -> str:
    path = os.path.join(directory, name)
    with open(path, "wb") as f:
        f.write(b"data")
    # backdate modification time
    mtime = time.time() - age_seconds
    os.utime(path, (mtime, mtime))
    return path


def test_cleanup_deletes_old_files(tmp_path, monkeypatch):
    monkeypatch.setattr("workers.cleanup_worker.AUDIO_DIR", str(tmp_path))
    monkeypatch.setattr("app.core.config.settings.AUDIO_FILE_TTL_SECONDS", 3600)

    old_file = _create_file(str(tmp_path), "old.mp3", age_seconds=7200)
    new_file = _create_file(str(tmp_path), "new.mp3", age_seconds=60)

    from workers.cleanup_worker import cleanup_audio
    result = cleanup_audio()

    assert result == {"deleted": 1}
    assert not os.path.exists(old_file)
    assert os.path.exists(new_file)


def test_cleanup_ignores_gitkeep(tmp_path, monkeypatch):
    monkeypatch.setattr("workers.cleanup_worker.AUDIO_DIR", str(tmp_path))
    monkeypatch.setattr("app.core.config.settings.AUDIO_FILE_TTL_SECONDS", 3600)

    _create_file(str(tmp_path), ".gitkeep", age_seconds=99999)

    from workers.cleanup_worker import cleanup_audio
    result = cleanup_audio()

    assert result == {"deleted": 0}
    assert os.path.exists(os.path.join(str(tmp_path), ".gitkeep"))


def test_cleanup_no_directory(tmp_path, monkeypatch):
    missing_dir = str(tmp_path / "nonexistent")
    monkeypatch.setattr("workers.cleanup_worker.AUDIO_DIR", missing_dir)

    from workers.cleanup_worker import cleanup_audio
    result = cleanup_audio()

    assert result == {"deleted": 0}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
pytest tests/workers/test_cleanup_worker.py -v
```
Expected: `ModuleNotFoundError: No module named 'workers.cleanup_worker'`

- [ ] **Step 3: Create the cleanup worker**

Create `workers/cleanup_worker.py`:
```python
import os
import time

from app.core.celery_app import celery_app
from app.core.config import settings

AUDIO_DIR = "audio_files"


@celery_app.task(name="workers.cleanup_worker.cleanup_audio")
def cleanup_audio() -> dict:
    if not os.path.exists(AUDIO_DIR):
        return {"deleted": 0}

    now = time.time()
    deleted = 0
    for fname in os.listdir(AUDIO_DIR):
        if fname == ".gitkeep":
            continue
        fpath = os.path.join(AUDIO_DIR, fname)
        if os.path.isfile(fpath) and (now - os.path.getmtime(fpath)) > settings.AUDIO_FILE_TTL_SECONDS:
            os.remove(fpath)
            deleted += 1
    return {"deleted": deleted}
```

- [ ] **Step 4: Add Celery beat schedule to celery_app.py**

In `app/core/celery_app.py`, add after `celery_app.conf.update(...)`:
```python
from celery.schedules import crontab

celery_app.conf.beat_schedule = {
    "cleanup-audio-files-hourly": {
        "task": "workers.cleanup_worker.cleanup_audio",
        "schedule": crontab(minute=0),  # top of every hour
    },
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
pytest tests/workers/test_cleanup_worker.py -v
```
Expected: 3 PASSED

- [ ] **Step 6: Commit**

```bash
git add workers/cleanup_worker.py app/core/celery_app.py tests/workers/test_cleanup_worker.py
git commit -m "feat: add cleanup Celery beat worker for stale audio files"
```

---

## Task 8: Static files mount and audio directory

**Files:**
- Modify: `app/main.py`
- Create: `audio_files/.gitkeep`
- Modify: `.gitignore` (Backend root)

- [ ] **Step 1: Add StaticFiles mount to main.py**

In `app/main.py`, add after the existing imports:
```python
import os
from fastapi.staticfiles import StaticFiles
```

Add after `app = FastAPI(...)`:
```python
os.makedirs("audio_files", exist_ok=True)
app.mount("/audio", StaticFiles(directory="audio_files"), name="audio")
```

- [ ] **Step 2: Create audio_files directory with gitkeep**

```bash
mkdir -p audio_files
touch audio_files/.gitkeep
```

- [ ] **Step 3: Add audio_files/*.mp3 to .gitignore**

Check if a `.gitignore` exists in the Backend directory. If so, append:
```
audio_files/*.mp3
audio_files/*.wav
```
If not, create it with those lines.

- [ ] **Step 4: Verify static files work**

Start the server and check the static mount resolves:
```bash
uvicorn app.main:app --reload
```
Then in another terminal:
```bash
curl http://localhost:8000/audio/ 
```
Expected: 404 (directory listing disabled) or 200 if an mp3 file exists — no 500 errors

- [ ] **Step 5: Run the full test suite**

```bash
pytest tests/ -v
```
Expected: all tests pass

- [ ] **Step 6: Commit**

```bash
git add app/main.py audio_files/.gitkeep .gitignore
git commit -m "feat: mount /audio static directory for TTS output files"
```

---

## Task 9: Docker GPU passthrough

**Files:**
- Modify: `docker-compose.yml`

This task is for when running the Celery worker inside Docker (optional — currently workers run directly on host).

- [ ] **Step 1: Add Celery worker service to docker-compose.yml**

Add the following service to `docker-compose.yml` (inside `services:`):
```yaml
  celery-worker:
    build: ./Backend
    command: celery -A app.core.celery_app worker -Q narrative -l info --concurrency 1
    env_file: ./Backend/.env
    volumes:
      - ./Backend:/app
      - tts_model_cache:/root/.local/share/tts
      - ./Backend/audio_files:/app/audio_files
    depends_on:
      redis:
        condition: service_healthy
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              capabilities: [gpu]
    runtime: nvidia
```

Add `tts_model_cache:` to the `volumes:` section at the bottom of `docker-compose.yml`.

- [ ] **Step 2: Create a Dockerfile for the Backend (if it doesn't exist)**

Create `Backend/Dockerfile`:
```dockerfile
FROM python:3.11-slim

RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .
```

- [ ] **Step 3: Commit**

```bash
git add docker-compose.yml Backend/Dockerfile
git commit -m "feat: add celery worker Docker service with NVIDIA GPU passthrough"
```

---

## Running the Celery worker locally (without Docker)

To test end-to-end without Docker:

```bash
# Terminal 1: FastAPI
uvicorn app.main:app --reload

# Terminal 2: Celery narrative worker
celery -A app.core.celery_app worker -Q narrative -l info --concurrency 1

# Terminal 3: Celery beat (for cleanup task)
celery -A app.core.celery_app beat -l info
```

End-to-end smoke test:
```bash
# 1. Get a token
TOKEN=$(curl -s -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"your@email.com","password":"yourpassword"}' | python -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# 2. Fire a narration job
TASK_ID=$(curl -s -X POST http://localhost:8000/narration/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"text":"Witamy w Warszawie!","language":"pl","speed":50,"pitch":50,"loudness":50}' | python -c "import sys,json; print(json.load(sys.stdin)['task_id'])")

# 3. Poll for result
curl -s http://localhost:8000/narration/$TASK_ID \
  -H "Authorization: Bearer $TOKEN"
# Expected: {"status":"done","audio_url":"/audio/<task_id>.mp3"}

# 4. Download the audio
curl http://localhost:8000/audio/<task_id>.mp3 -o test_narration.mp3
```
