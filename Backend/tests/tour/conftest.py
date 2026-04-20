"""
Local conftest for tour tests.

The route_store tests use only mocks and do not need a live database.
Override the session-scoped autouse fixtures from the root conftest so
these tests can run without Docker / a test-DB connection.
"""
import pytest


@pytest.fixture(scope="session", autouse=True)
async def setup_database():
    """No-op override — tour unit tests use mocks, not a real DB."""
    yield


@pytest.fixture(scope="session", autouse=True)
async def override_get_db(setup_database):
    """No-op override — tour unit tests use mocks, not a real DB."""
    yield
