import pytest
import uuid
from httpx import AsyncClient, ASGITransport
from app.main import app
from asgi_lifespan import LifespanManager

@pytest.mark.asyncio
async def test_update_user_preferences():
    async with LifespanManager(app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            email = f"prefs_{uuid.uuid4()}@example.com"
            # Register
            response = await ac.post("/auth/register", json={
                "email": email,
                "password": "testpassword"
            })
            tokens = response.json()
            headers = {"Authorization": f"Bearer {tokens['access_token']}"}
            # Update preferences (przykład: language)
            response = await ac.put("/user/preferences", json={"": "en"}, headers=headers)
            assert response.status_code in (200, 204)
