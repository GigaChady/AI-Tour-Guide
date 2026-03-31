
import pytest
import asyncio
from typing import Generator
import os
import sys
import uuid
import sqlalchemy
import pytest
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.pool import NullPool
from app.models import Base
from app.main import app
from app.core.database import get_db

TEST_DATABASE_URL = os.getenv("TEST_DATABASE_URL", "postgresql+asyncpg://postgres:postgres@localhost:5433/test_db")

engine_test = create_async_engine(TEST_DATABASE_URL, echo=False, poolclass=NullPool)
AsyncSessionTest = async_sessionmaker(engine_test, class_=AsyncSession, expire_on_commit=False)

@pytest.fixture(scope="session")
def event_loop() -> Generator:
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()

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

# Override get_db for all tests
@pytest.fixture(autouse=True)
def override_get_db(db_session, monkeypatch):
    async def _get_db_override():
        yield db_session
    monkeypatch.setattr("app.core.database.get_db", _get_db_override)