# TTS Integration Design

**Date:** 2026-04-07  
**Status:** Approved

## Overview

Integrate Text-to-Speech into the AI Tour Guide backend. The backend synthesizes narration text into audio files and serves them to the frontend via a static file URL. The system is built around a provider abstraction so the current open-source backend (Coqui XTTS2) can be swapped to Google Cloud TTS with a single env var change.

---

## Architecture

```
POST /narration/generate
  → fires Celery task (narrative queue)
  → returns { task_id }

GET /narration/{task_id}
  → polls Celery result
  → returns { status: "pending"|"processing"|"done"|"failed", audio_url: "/audio/{task_id}.mp3" }

Celery narrative worker
  → generates narration text (LLM)
  → calls TTSService.synthesize(text, language, speed, pitch, loudness)
  → saves audio to audio_files/{task_id}.mp3
  → stores result URL in Redis via Celery backend

FastAPI static mount
  → /audio → ./audio_files/
  → serves .mp3 files directly to frontend
```

### TTS service layout

```
app/services/tts/
  __init__.py
  base.py        — abstract TTSProvider with synthesize() interface
  coqui.py       — CoquiTTSProvider (XTTS2, active)
  google.py      — GoogleTTSProvider (future migration)
  factory.py     — returns provider based on TTS_PROVIDER env var
```

---

## Provider: Coqui XTTS2 (current)

- Library: `TTS` (pip), model: `tts_models/multilingual/multi-dataset/xtts_v2`
- Runs in the Celery worker process with NVIDIA GPU passthrough via Docker
- Model downloads automatically on first use (~2GB), cached in Docker volume
- Languages: Polish (`pl`), English (`en`), and others from the preference catalog

**Parameter mapping (user preference 0–100 → TTS parameter):**

| Preference | XTTS2 | Range |
|---|---|---|
| `speed` | `speed` (native) | 0–100 → 0.5–2.0 |
| `pitch` | `pydub` semitone shift (post-process) | 0–100 → -6 to +6 semitones |
| `loudness` | `pydub` dB gain (post-process) | 0–100 → -10 to +10 dB |

---

## Provider: Google Cloud TTS (future)

- Library: `google-cloud-texttospeech`
- Voices: `pl-PL-Wavenet-A/B/C/D`, plus English Neural voices
- Free tier: 4M WaveNet chars/month — comfortable for ≤4 users
- Requires `GOOGLE_APPLICATION_CREDENTIALS` env var (service account JSON)

**Parameter mapping:**

| Preference | Google TTS | Range |
|---|---|---|
| `speed` | `speakingRate` | 0–100 → 0.25–4.0 |
| `pitch` | `pitch` (semitones) | 0–100 → -20 to +20 |
| `loudness` | `volumeGainDb` | 0–100 → -10 to +10 dB |

Migration: set `TTS_PROVIDER=google` in `.env`, add `GOOGLE_APPLICATION_CREDENTIALS`. No code changes.

---

## Audio Delivery

1. Worker saves synthesized audio to `audio_files/{task_id}.mp3`
2. FastAPI mounts `./audio_files` at `/audio`
3. Frontend receives `{ "audio_url": "/audio/{task_id}.mp3" }` and fetches directly
4. Files are cleaned up after `AUDIO_FILE_TTL_SECONDS` (default: 3600) via a periodic Celery beat task

---

## Docker Changes

Add GPU passthrough to the Celery worker service in `docker-compose.yml`:

```yaml
deploy:
  resources:
    reservations:
      devices:
        - driver: nvidia
          capabilities: [gpu]
```

Add a volume for the XTTS2 model cache so it survives container restarts:

```yaml
volumes:
  - tts_model_cache:/root/.local/share/tts
```

---

## Config additions (Settings)

```
TTS_PROVIDER=coqui              # or "google"
TTS_SPEAKER_WAV=                # optional: path to reference speaker .wav for XTTS2 voice cloning
                                # if unset, a bundled default speaker wav (neutral Polish/English) is used
AUDIO_FILE_TTL_SECONDS=3600
GOOGLE_APPLICATION_CREDENTIALS= # only needed when TTS_PROVIDER=google
```

---

## New files

| File | Purpose |
|---|---|
| `app/services/tts/base.py` | Abstract `TTSProvider` |
| `app/services/tts/coqui.py` | Coqui XTTS2 implementation |
| `app/services/tts/google.py` | Google Cloud TTS implementation (stub, filled later) |
| `app/services/tts/factory.py` | Provider factory |
| `workers/narrative_worker.py` | Celery task: LLM → TTS → save audio |
| `workers/cleanup_worker.py` | Celery beat task: delete audio files older than `AUDIO_FILE_TTL_SECONDS` |
| `audio_files/` | Runtime audio output directory (gitignored) |

## Modified files

| File | Change |
|---|---|
| `app/routers/narration.py` | Implement `/generate` and `/{task_id}` endpoints |
| `app/main.py` | Mount `/audio` static directory |
| `app/core/config.py` | Add `TTS_PROVIDER`, `AUDIO_FILE_TTL_SECONDS`, `GOOGLE_APPLICATION_CREDENTIALS` |
| `docker-compose.yml` | Add GPU passthrough + model cache volume |
| `.env.example` | Add new env vars |
