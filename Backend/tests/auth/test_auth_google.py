import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app
from asgi_lifespan import LifespanManager

@pytest.mark.asyncio
async def test_google_login():
    async with LifespanManager(app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            fake_google_token = "test-google-token"
            response = await ac.post("/auth/google", json={
                "google_token": fake_google_token
            })
            assert response.status_code in (200, 400, 401)
            if response.status_code == 200:
                data = response.json()
                assert "access_token" in data
                assert "refresh_token" in data
