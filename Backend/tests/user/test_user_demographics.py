import pytest
import uuid
from httpx import AsyncClient, ASGITransport
from app.main import app
from asgi_lifespan import LifespanManager


@pytest.mark.asyncio
async def test_update_and_get_user_demographics():
    async with LifespanManager(app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            email = f"demo_{uuid.uuid4()}@example.com"
            response = await ac.post("/auth/register", json={
                "email": email,
                "password": "Testpass1"
            })
            tokens = response.json()
            headers = {"Authorization": f"Bearer {tokens['access_token']}"}

            response = await ac.get("/user/demographics/questions")
            assert response.status_code == 200
            questions = response.json()
            assert any(item["question_key"] == "gender" for item in questions)
            assert any(item["question_key"] == "age" for item in questions)

            response = await ac.post("/user/demographics/answers", json=[
                {
                    "question_key": "gender",
                    "answer_id": "male"
                },
                {
                    "question_key": "age",
                    "value": 29
                }
            ], headers=headers)
            assert response.status_code == 204
