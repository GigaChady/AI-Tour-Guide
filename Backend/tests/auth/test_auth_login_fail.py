import pytest
import uuid
from httpx import AsyncClient, ASGITransport
from app.main import app
from asgi_lifespan import LifespanManager

# @pytest.mark.asyncio
# async def test_login_wrong_password():
#     async with LifespanManager(app):
#         transport = ASGITransport(app=app)
#         async with AsyncClient(transport=transport, base_url="http://test") as ac:
#             email = f"fail_{uuid.uuid4()}@example.com"
#             # Register
#             await ac.post("/auth/register", json={
#                 "email": email,
#                 "password": "testpassword"
#             })
#             # Login with wrong password
#             response = await ac.post("/auth/login", json={
#                 "email": email,
#                 "password": "wrongpassword"
#             })
#             assert response.status_code == 401

@pytest.mark.asyncio
async def test_login_nonexistent_user():
    async with LifespanManager(app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            email = f"nonexistent_{uuid.uuid4()}@example.com"
            response = await ac.post("/auth/login", json={
                "email": email,
                "password": "irrelevant"
            })
            assert response.status_code == 401
