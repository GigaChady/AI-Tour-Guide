import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app
from asgi_lifespan import LifespanManager

@pytest.mark.asyncio
async def test_register_invalid_email():
    async with LifespanManager(app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            response = await ac.post("/auth/register", json={
                "email": "not-an-email",
                "password": "Testpass1"
            })
            assert response.status_code in (400, 422)

@pytest.mark.asyncio
async def test_register_empty_data():
    async with LifespanManager(app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            response = await ac.post("/auth/register", json={})
            assert response.status_code in (400, 422)
