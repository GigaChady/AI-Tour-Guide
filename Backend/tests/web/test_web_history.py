import uuid
import pytest
from datetime import datetime, timezone, timedelta
from httpx import AsyncClient, ASGITransport
from asgi_lifespan import LifespanManager
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.pool import NullPool
from jose import jwt as jose_jwt
from app.main import app
from app.models.models import Route

_TEST_DB_URL = "postgresql+asyncpg://postgres:postgres@localhost:5433/test_db"
_engine = create_async_engine(_TEST_DB_URL, poolclass=NullPool)
_Session = async_sessionmaker(_engine, class_=AsyncSession, expire_on_commit=False)


async def _register(ac: AsyncClient, email: str) -> tuple[str, str]:
    r = await ac.post("/auth/register", json={"email": email, "password": "Testpass1", "name": "Test User"})
    assert r.status_code == 200, r.text
    token = r.json()["access_token"]
    user_id = jose_jwt.get_unverified_claims(token)["sub"]
    return token, user_id


async def _insert_route(user_id: str, distance_m: float = 1500.0, city: str = "Warsaw",
                        country: str = "Poland", minutes_ago: int = 10) -> str:
    now = datetime.now(timezone.utc).replace(tzinfo=None)
    start = now - timedelta(minutes=minutes_ago + 30)
    end = now - timedelta(minutes=minutes_ago)
    async with _Session() as session:
        route = Route(
            user_id=uuid.UUID(user_id),
            started_at=start,
            ended_at=end,
            distance_m=distance_m,
            city=city,
            country=country,
        )
        session.add(route)
        await session.commit()
        return str(route.id)


@pytest.mark.asyncio
async def test_history_empty():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"hist_empty_{uuid.uuid4()}@example.com"
            token, _ = await _register(ac, email)

            r = await ac.get("/web/history", headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 200
            data = r.json()
            assert data["user"]["total_explorations"] == 0
            assert data["user"]["total_distance_km"] == 0.0
            assert data["routes"] == []


@pytest.mark.asyncio
async def test_history_with_routes():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"hist_routes_{uuid.uuid4()}@example.com"
            token, user_id = await _register(ac, email)

            await _insert_route(user_id, distance_m=2000.0, city="Krakow")
            await _insert_route(user_id, distance_m=3000.0, city="Warsaw")

            r = await ac.get("/web/history", headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 200
            data = r.json()
            assert data["user"]["total_explorations"] == 2
            assert data["user"]["total_distance_km"] == pytest.approx(5.0, rel=0.01)
            assert len(data["routes"]) == 2


@pytest.mark.asyncio
async def test_history_unauthorized():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            r = await ac.get("/web/history")
            assert r.status_code == 401


@pytest.mark.asyncio
async def test_dashboard_empty():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"dash_empty_{uuid.uuid4()}@example.com"
            token, _ = await _register(ac, email)

            r = await ac.get("/web/dashboard", headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 200
            data = r.json()
            assert data["stats"]["total_countries"] == 0
            assert data["stats"]["total_cities"] == 0
            assert data["recent_expeditions"] == []


@pytest.mark.asyncio
async def test_dashboard_with_routes():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"dash_routes_{uuid.uuid4()}@example.com"
            token, user_id = await _register(ac, email)

            await _insert_route(user_id, city="Berlin", country="Germany")
            await _insert_route(user_id, city="Paris", country="France")

            r = await ac.get("/web/dashboard", headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 200
            data = r.json()
            assert data["stats"]["total_countries"] == 2
            assert data["stats"]["total_cities"] == 2
            assert len(data["recent_expeditions"]) == 2


@pytest.mark.asyncio
async def test_dashboard_unauthorized():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            r = await ac.get("/web/dashboard")
            assert r.status_code == 401
