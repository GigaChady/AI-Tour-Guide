import pytest
import uuid
from httpx import AsyncClient, ASGITransport
from app.main import app
from asgi_lifespan import LifespanManager

@pytest.mark.asyncio
async def test_refresh_token():
    async with LifespanManager(app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            email = f"refresh_{uuid.uuid4()}@example.com"
            response = await ac.post("/auth/register", json={
                "email": email,
                "password": "Testpass1",
                "name": "Test User"
            })
            assert response.status_code == 200
            data = response.json()
            refresh_token = data["refresh_token"]

            response = await ac.post("/auth/refresh", json={
                "refresh_token": refresh_token
            })
            assert response.status_code == 200
            data = response.json()
            assert "access_token" in data
            assert "refresh_token" in data