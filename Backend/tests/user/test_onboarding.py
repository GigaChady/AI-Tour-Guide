import uuid
import pytest
from httpx import AsyncClient, ASGITransport
from asgi_lifespan import LifespanManager
from app.main import app

_VALID_ANSWERS = {
    "items": [
        {"question_key": "gender", "answer_key": "male"},
        {"question_key": "interests", "answer_keys": ["history", "nature"]},
    ]
}


async def _register(ac: AsyncClient, email: str) -> str:
    r = await ac.post("/auth/register", json={"email": email, "password": "Testpass1", "name": "Test User"})
    assert r.status_code == 200, r.text
    return r.json()["access_token"]


@pytest.mark.asyncio
async def test_get_questions_pl():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"ob_pl_{uuid.uuid4()}@example.com"
            token = await _register(ac, email)

            r = await ac.get("/user/onboarding/questions?lang=pl",
                             headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 200
            data = r.json()
            keys = [q["key"] for q in data["items"]]
            assert "gender" in keys
            assert "interests" in keys


@pytest.mark.asyncio
async def test_get_questions_en():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"ob_en_{uuid.uuid4()}@example.com"
            token = await _register(ac, email)

            r = await ac.get("/user/onboarding/questions?lang=en",
                             headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 200
            data = r.json()
            gender_q = next(q for q in data["items"] if q["key"] == "gender")
            assert gender_q["title"] == "What is your gender?"


@pytest.mark.asyncio
async def test_get_questions_unauthorized():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            r = await ac.get("/user/onboarding/questions")
            assert r.status_code == 401


@pytest.mark.asyncio
async def test_save_valid_answers():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"ob_save_{uuid.uuid4()}@example.com"
            token = await _register(ac, email)

            r = await ac.post("/user/onboarding/answers", json=_VALID_ANSWERS,
                              headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 204


@pytest.mark.asyncio
async def test_save_answers_missing_question():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"ob_missing_{uuid.uuid4()}@example.com"
            token = await _register(ac, email)

            r = await ac.post("/user/onboarding/answers",
                              json={"items": [{"question_key": "gender", "answer_key": "male"}]},
                              headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 422


@pytest.mark.asyncio
async def test_save_answers_invalid_answer_key():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"ob_inv_{uuid.uuid4()}@example.com"
            token = await _register(ac, email)

            r = await ac.post("/user/onboarding/answers",
                              json={"items": [
                                  {"question_key": "gender", "answer_key": "invalid_option"},
                                  {"question_key": "interests", "answer_keys": ["history"]},
                              ]},
                              headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 422


@pytest.mark.asyncio
async def test_save_answers_empty_list():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"ob_emp_{uuid.uuid4()}@example.com"
            token = await _register(ac, email)

            r = await ac.post("/user/onboarding/answers", json={"items": []},
                              headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 422


@pytest.mark.asyncio
async def test_get_questions_shows_saved_answers():
    async with LifespanManager(app):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            email = f"ob_sel_{uuid.uuid4()}@example.com"
            token = await _register(ac, email)

            await ac.post("/user/onboarding/answers", json=_VALID_ANSWERS,
                          headers={"Authorization": f"Bearer {token}"})

            r = await ac.get("/user/onboarding/questions?lang=pl",
                             headers={"Authorization": f"Bearer {token}"})
            assert r.status_code == 200
            data = r.json()
            selected = data["selected_answers"]
            assert selected["gender"] == "male"
            assert "history" in selected["interests"]
            assert "nature" in selected["interests"]
