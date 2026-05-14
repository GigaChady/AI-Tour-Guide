import uuid
import pytest
from httpx import AsyncClient, ASGITransport
from asgi_lifespan import LifespanManager
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.pool import NullPool
from jose import jwt as jose_jwt
from app.main import app
from app.models.models import User

_TEST_DB_URL = "postgresql+asyncpg://postgres:postgres@localhost:5433/test_db"
_engine = create_async_engine(_TEST_DB_URL, poolclass=NullPool)
_Session = async_sessionmaker(_engine, class_=AsyncSession, expire_on_commit=False)


async def _register(ac: AsyncClient, email: str, password: str = "Testpass1", name: str = "Test User") -> tuple[str, str]:
    r = await ac.post("/auth/register", json={"email": email, "password": password, "name": name})
    assert r.status_code == 200, r.text
    token = r.json()["access_token"]
    user_id = jose_jwt.get_unverified_claims(token)["sub"]
    return token, user_id


async def _make_admin(user_id: str) -> None:
    async with _Session() as session:
        user = await session.get(User, uuid.UUID(user_id))
        user.is_admin = True
        await session.commit()


@pytest.mark.asyncio
async def test_get_own_user():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"crud_self_{uuid.uuid4()}@example.com"
            token, user_id = await _register(ac, email, name="Self User")

            r = await ac.get(f"/user/{user_id}", headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 200
            assert r.json()["email"] == email


@pytest.mark.asyncio
async def test_get_other_user_forbidden():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email1 = f"crud_a_{uuid.uuid4()}@example.com"
            email2 = f"crud_b_{uuid.uuid4()}@example.com"
            token1, _ = await _register(ac, email1)
            _, user_id2 = await _register(ac, email2)

            r = await ac.get(f"/user/{user_id2}", headers={"Authorization": f"Bearer {token1}"})
            assert r.status_code == 403


@pytest.mark.asyncio
async def test_get_user_not_found():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"crud_nf_{uuid.uuid4()}@example.com"
            token, _ = await _register(ac, email)
            random_id = uuid.uuid4()

            r = await ac.get(f"/user/{random_id}", headers={"Authorization": f"Bearer {token}"})
            assert r.status_code in (403, 404)


@pytest.mark.asyncio
async def test_update_own_name():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"crud_upd_{uuid.uuid4()}@example.com"
            token, user_id = await _register(ac, email, name="Old Name")

            r = await ac.patch(f"/user/{user_id}", json={"name": "Updated Name"},
                               headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 204

            r = await ac.get(f"/user/{user_id}", headers={"Authorization": f"Bearer {token}"})
            assert r.json()["name"] == "Updated Name"


@pytest.mark.asyncio
async def test_update_other_user_forbidden():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email1 = f"crud_ua_{uuid.uuid4()}@example.com"
            email2 = f"crud_ub_{uuid.uuid4()}@example.com"
            token1, _ = await _register(ac, email1)
            _, user_id2 = await _register(ac, email2)

            r = await ac.patch(f"/user/{user_id2}", json={"name": "Hacker"},
                               headers={"Authorization": f"Bearer {token1}"})
            assert r.status_code == 403


@pytest.mark.asyncio
async def test_delete_own_user():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"crud_del_{uuid.uuid4()}@example.com"
            token, user_id = await _register(ac, email)

            r = await ac.delete(f"/user/delete/{user_id}",
                                headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 204


@pytest.mark.asyncio
async def test_delete_other_user_forbidden():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email1 = f"crud_da_{uuid.uuid4()}@example.com"
            email2 = f"crud_db_{uuid.uuid4()}@example.com"
            token1, _ = await _register(ac, email1)
            _, user_id2 = await _register(ac, email2)

            r = await ac.delete(f"/user/delete/{user_id2}",
                                headers={"Authorization": f"Bearer {token1}"})
            assert r.status_code == 403


@pytest.mark.asyncio
async def test_admin_get_any_user():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            admin_email = f"admin_get_{uuid.uuid4()}@example.com"
            target_email = f"target_get_{uuid.uuid4()}@example.com"
            admin_token, admin_id = await _register(ac, admin_email)
            _, target_id = await _register(ac, target_email)

            await _make_admin(admin_id)

            r = await ac.get(f"/user/{target_id}", headers={"Authorization": f"Bearer {admin_token}"})
            assert r.status_code == 200


@pytest.mark.asyncio
async def test_admin_create_user():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            admin_email = f"admin_add_{uuid.uuid4()}@example.com"
            admin_token, admin_id = await _register(ac, admin_email)
            await _make_admin(admin_id)

            new_email = f"created_{uuid.uuid4()}@example.com"
            r = await ac.post("/user/add",
                              json={"email": new_email, "password": "Testpass1", "name": "Created User"},
                              headers={"Authorization": f"Bearer {admin_token}"})
            assert r.status_code == 201


@pytest.mark.asyncio
async def test_non_admin_cannot_create_user():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"nonadmin_{uuid.uuid4()}@example.com"
            token, _ = await _register(ac, email)

            new_email = f"blocked_{uuid.uuid4()}@example.com"
            r = await ac.post("/user/add",
                              json={"email": new_email, "password": "Testpass1", "name": "Blocked"},
                              headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 403
