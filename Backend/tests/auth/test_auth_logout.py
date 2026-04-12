import pytest
import uuid
from httpx import AsyncClient, ASGITransport
from app.main import app
from asgi_lifespan import LifespanManager

@pytest.mark.asyncio
async def test_logout_revokes_refresh_token():
    async with LifespanManager(app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            email = f"logout_{uuid.uuid4()}@example.com"
            # Register
            response = await ac.post("/auth/register", json={
                "email": email,
                "password": "Testpass1"
            })
            tokens = response.json()
            refresh_token = tokens["refresh_token"]
            # Logout (revoke refresh token)
            logout_response = await ac.post("/auth/logout", json={"refresh_token": refresh_token})
            assert logout_response.status_code == 204
            # Try to use revoked refresh token
            response = await ac.post("/auth/refresh", json={"refresh_token": refresh_token})
            assert response.status_code in (401, 400)
