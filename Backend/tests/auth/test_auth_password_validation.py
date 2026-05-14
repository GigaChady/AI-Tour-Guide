import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app
from asgi_lifespan import LifespanManager

@pytest.mark.asyncio
async def test_register_short_password():
    async with LifespanManager(app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            response = await ac.post("/auth/register", json={
                "email": "shortpass@example.com",
                "password": "123",
                "name": "Test User"
            })
            assert response.status_code == 400
