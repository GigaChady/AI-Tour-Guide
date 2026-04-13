import pytest
import uuid
from httpx import AsyncClient, ASGITransport
from app.main import app
from asgi_lifespan import LifespanManager

@pytest.mark.asyncio
async def test_get_preferences_questions():
    async with LifespanManager(app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            response = await ac.get("/user/preferences/questions")
            assert response.status_code == 200

            questions = response.json()
            assert len(questions) == 5
            assert questions[0]["question_id"] == 1
            assert questions[0]["question_key"] == "pitch"
            assert questions[0]["type"] == "percentage"
            assert questions[0]["input"] == {"min": 0, "max": 100, "required": True}
            assert questions[3]["type"] == "single_choice"
            assert len(questions[3]["answers"]) == 10
            assert questions[4]["question_key"] == "interests"
            assert questions[4]["type"] == "multi_choice"
            assert questions[4]["answers"][0]["answer_id"] == "architecture"
            assert questions[4]["answers"][0]["trailingContent"] == "🏛️"


@pytest.mark.asyncio
async def test_save_user_preferences_answers_single_and_multi():
    async with LifespanManager(app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            email = f"prefs_{uuid.uuid4()}@example.com"
            response = await ac.post("/auth/register", json={
                "email": email,
                "password": "Testpass1"
            })
            tokens = response.json()
            headers = {"Authorization": f"Bearer {tokens['access_token']}"}

            response = await ac.post(
                "/user/preferences/answers",
                json=[
                    {
                        "question_id": 1,
                        "value": 75
                    },
                    {
                        "question_id": 5,
                        "answer_ids": ["architecture", "nature"]
                    }
                ],
                headers=headers,
            )

            assert response.status_code == 200
            assert response.json() == [
                {
                    "question_id": 1,
                    "answer_id": None,
                    "answer_ids": None,
                    "value": 75,
                },
                {
                    "question_id": 5,
                    "answer_id": None,
                    "answer_ids": ["architecture", "nature"],
                    "value": None,
                },
            ]
