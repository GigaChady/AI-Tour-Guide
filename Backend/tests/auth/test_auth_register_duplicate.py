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
            # Register first time
            await ac.post("/auth/register", json={
                "email": email,
                "password": "testpassword"
            })
            # Register duplicate
            response = await ac.post("/auth/register", json={
                "email": email,
                "password": "testpassword"
            })
            assert response.status_code == 400
            assert "Email already in use" in response.text