
import pytest
import os
import sqlalchemy
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.pool import NullPool


def _load_env_file(path: str) -> None:
    if not os.path.exists(path):
        return

    with open(path, "r", encoding="utf-8") as f:
        for raw_line in f:
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            os.environ.setdefault(key.strip(), value.strip())


_load_env_file(os.path.join(os.path.dirname(__file__), "..", ".env.test"))

TEST_DATABASE_URL = os.getenv("TEST_DATABASE_URL") or os.getenv("DATABASE_URL")
if not TEST_DATABASE_URL:
    raise RuntimeError("Missing TEST_DATABASE_URL or DATABASE_URL for tests")

os.environ["DATABASE_URL"] = TEST_DATABASE_URL

from app.models import Base
from app.main import app
from app.core.database import get_db

engine_test = create_async_engine(TEST_DATABASE_URL, echo=False, poolclass=NullPool)
AsyncSessionTest = async_sessionmaker(engine_test, class_=AsyncSession, expire_on_commit=False)


@pytest.fixture(scope="session", autouse=True)
async def prepare_database():
    async with engine_test.begin() as conn:
        await conn.execute(sqlalchemy.text("CREATE EXTENSION IF NOT EXISTS postgis"))
        await conn.run_sync(Base.metadata.drop_all)
        await conn.run_sync(Base.metadata.create_all)
    yield
    async with engine_test.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)


@pytest.fixture(scope="function")
async def db_session():
    async with AsyncSessionTest() as session:
        yield session
        await session.rollback()


@pytest.fixture(autouse=True)
def patch_startup(monkeypatch):
    async def _noop():
        return None

    monkeypatch.setattr("app.main.init_db", _noop)
    monkeypatch.setattr("app.main.init_redis", _noop)
    monkeypatch.setattr("app.main.close_redis", _noop)


@pytest.fixture(autouse=True)
async def override_get_db(db_session):
    async def _get_db_override():
        yield db_session

    app.dependency_overrides[get_db] = _get_db_override
    yield
    app.dependency_overrides.pop(get_db, None)
