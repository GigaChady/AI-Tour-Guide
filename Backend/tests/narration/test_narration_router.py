import pytest
from httpx import AsyncClient, ASGITransport
from unittest.mock import MagicMock
from app.main import app


async def _get_auth_headers(client: AsyncClient) -> dict:
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
