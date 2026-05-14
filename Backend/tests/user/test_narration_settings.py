import uuid
import pytest
from httpx import AsyncClient, ASGITransport
from asgi_lifespan import LifespanManager
from app.main import app

_VALID_SETTINGS = {
    "language": "pl",
    "pitch": 50,
    "speed": 5,
    "detail_level": "normal",
    "auto_play": False,
}


async def _register(ac: AsyncClient, email: str) -> str:
    r = await ac.post("/auth/register", json={"email": email, "password": "Testpass1", "name": "Test User"})
    assert r.status_code == 200, r.text
    return r.json()["access_token"]


@pytest.mark.asyncio
async def test_get_narration_settings_default_empty():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"ns_empty_{uuid.uuid4()}@example.com"
            token = await _register(ac, email)

            r = await ac.get("/user/narration-settings", headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 200
            assert r.json() == {}


@pytest.mark.asyncio
async def test_save_and_get_narration_settings():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"ns_save_{uuid.uuid4()}@example.com"
            token = await _register(ac, email)

            r = await ac.post("/user/narration-settings", json=_VALID_SETTINGS,
                              headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 204

            r = await ac.get("/user/narration-settings", headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 200
            data = r.json()
            assert data["language"] == "pl"
            assert data["pitch"] == 50
            assert data["speed"] == 5
            assert data["auto_play"] is False


@pytest.mark.asyncio
async def test_update_narration_settings_overwrites():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"ns_upd_{uuid.uuid4()}@example.com"
            token = await _register(ac, email)

            await ac.post("/user/narration-settings", json=_VALID_SETTINGS,
                          headers={"Authorization": f"Bearer {token}"})

            updated = {**_VALID_SETTINGS, "language": "en", "pitch": 70, "auto_play": True}
            r = await ac.post("/user/narration-settings", json=updated,
                              headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 204

            r = await ac.get("/user/narration-settings", headers={"Authorization": f"Bearer {token}"})
            data = r.json()
            assert data["language"] == "en"
            assert data["pitch"] == 70
            assert data["auto_play"] is True


@pytest.mark.asyncio
async def test_narration_settings_unauthorized():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            r = await ac.get("/user/narration-settings")
            assert r.status_code == 401

            r = await ac.post("/user/narration-settings", json=_VALID_SETTINGS)
            assert r.status_code == 401
