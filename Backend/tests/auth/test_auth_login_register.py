import sys

import pytest
import uuid
from httpx import AsyncClient, ASGITransport
from app.main import app
from asgi_lifespan import LifespanManager
import asyncio

@pytest.mark.asyncio
async def test_register_and_login():
    async with LifespanManager(app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
                email = f"test_{uuid.uuid4()}@example.com"
                # Register
                response = await ac.post("/auth/register", json={
                    "email": email,
                    "password": "Testpass1"
                })
                assert response.status_code == 200
                data = response.json()
                assert "access_token" in data
                assert "refresh_token" in data

                # Login
                response = await ac.post("/auth/login", json={
                    "email": email,
                    "password": "Testpass1"
                })
                assert response.status_code == 200
                data = response.json()
                assert "access_token" in data
                assert "refresh_token" in data

