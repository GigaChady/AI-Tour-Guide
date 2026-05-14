import pytest
import uuid
from httpx import AsyncClient, ASGITransport
from app.main import app
from asgi_lifespan import LifespanManager

@pytest.mark.asyncio
async def test_register_duplicate():
    async with LifespanManager(app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            email = f"dupe_{uuid.uuid4()}@example.com"
            await ac.post("/auth/register", json={
                "email": email,
                "password": "Testpass1",
                "name": "Test User"
            })
            response = await ac.post("/auth/register", json={
                "email": email,
                "password": "Testpass1",
                "name": "Test User"
            })
            assert response.status_code == 409
            assert "Email already in use" in response.text