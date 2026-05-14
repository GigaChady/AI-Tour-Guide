import uuid
import pytest
from httpx import AsyncClient, ASGITransport
from asgi_lifespan import LifespanManager
from app.main import app


async def _register(ac: AsyncClient, email: str, password: str = "Testpass1", name: str = "Test User") -> str:
    r = await ac.post("/auth/register", json={"email": email, "password": password, "name": name})
    assert r.status_code == 200, r.text
    return r.json()["access_token"]


@pytest.mark.asyncio
async def test_get_me_params():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"me_{uuid.uuid4()}@example.com"
            token = await _register(ac, email, name="Alice")
            r = await ac.get("/user/me-params", headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 200
            data = r.json()
            assert data["email"] == email
            assert data["name"] == "Alice"


@pytest.mark.asyncio
async def test_get_me_params_unauthorized():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            r = await ac.get("/user/me-params")
            assert r.status_code == 401


@pytest.mark.asyncio
async def test_update_password_too_weak():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"weakpw_{uuid.uuid4()}@example.com"
            token = await _register(ac, email)

            r = await ac.patch("/user/update-params", json={"new_password": "weak"},
                               headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 400


@pytest.mark.asyncio
async def test_update_email_same_as_current_rejected():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"samemail_{uuid.uuid4()}@example.com"
            token = await _register(ac, email)

            r = await ac.patch("/user/update-params", json={"new_email": email},
                               headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 400


@pytest.mark.asyncio
async def test_update_email_already_taken():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email1 = f"taken1_{uuid.uuid4()}@example.com"
            email2 = f"taken2_{uuid.uuid4()}@example.com"
            token1 = await _register(ac, email1)
            await _register(ac, email2)

            r = await ac.patch("/user/update-params", json={"new_email": email2},
                               headers={"Authorization": f"Bearer {token1}"})
            assert r.status_code == 409
