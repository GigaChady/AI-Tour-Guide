from unittest.mock import patch
from fastapi.testclient import TestClient
from fastapi import FastAPI

from app.routers.audio import router

app = FastAPI()
app.include_router(router)
client = TestClient(app)


def test_get_m3u8_returns_200(tmp_path):
    m3u8_file = tmp_path / "index.m3u8"
    m3u8_file.write_text("#EXTM3U\n")

    with patch("app.routers.audio.audio_store.get", return_value=tmp_path):
        response = client.get("/audio/test-id/index.m3u8")

    assert response.status_code == 200
    assert response.headers["content-type"] == "application/vnd.apple.mpegurl"


def test_get_ts_segment_returns_200(tmp_path):
    seg_file = tmp_path / "seg000.ts"
    seg_file.write_bytes(b"\x00\x01\x02")

    with patch("app.routers.audio.audio_store.get", return_value=tmp_path):
        response = client.get("/audio/test-id/seg000.ts")

    assert response.status_code == 200
    assert response.headers["content-type"] == "video/mp2t"


def test_unknown_narration_id_returns_404():
    with patch("app.routers.audio.audio_store.get", return_value=None):
        response = client.get("/audio/unknown-id/index.m3u8")

    assert response.status_code == 404


def test_missing_file_returns_404(tmp_path):
    with patch("app.routers.audio.audio_store.get", return_value=tmp_path):
        response = client.get("/audio/test-id/seg999.ts")

    assert response.status_code == 404
