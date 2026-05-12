from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import DEFAULT_ONBOARDING_CATALOG, DEFAULT_ONBOARDING_CATALOG_EN
from app.core.database import get_db
from app.core.dependencies import get_current_user
from app.models.models import User, UserPreferences
from app.schemas.schemas import (
    ErrorResponse,
    OnboardingAnswerRequest,
    OnboardingAnswersRequest,
    OnboardingOption,
    OnboardingQuestion,
    OnboardingQuestionsResponse,
    OnboardingSelectedAnswers,
)

router = APIRouter(prefix="/user", tags=["user"])

_CATALOGS = {
    "pl": DEFAULT_ONBOARDING_CATALOG,
    "en": DEFAULT_ONBOARDING_CATALOG_EN,
}


def _options_from_catalog(question_key: str, catalog: list) -> dict:
    item = next((i for i in catalog if i["question_key"] == question_key), None)
    if not item:
        return {"items": [], "detail": None}
    return {
        "items": [
            OnboardingOption(
                key=str(a["answer_key"]),
                title=str(a["title"]),
                body=a.get("body") if isinstance(a.get("body"), str) else None,
                trailing_content=a.get("trailing_content") if isinstance(a.get("trailing_content"), str) else None,
            )
            for a in item.get("answers", [])
        ],
        "detail": None
    }


def _build_questions(catalog: list) -> dict:
    gender_catalog = next(i for i in catalog if i["question_key"] == "gender")
    interests_catalog = next(i for i in catalog if i["question_key"] == "interests")
    return {
        "items": [
            OnboardingQuestion(key="gender", title=gender_catalog["title"], type="single_choice", options=_options_from_catalog("gender", catalog)["items"]),
            OnboardingQuestion(key="interests", title=interests_catalog["title"], type="multi_choice", options=_options_from_catalog("interests", catalog)["items"]),
        ],
        "detail": None
    }


def _normalize_onboarding_answers(
    answers: list[OnboardingAnswerRequest],
    questions: dict,
) -> dict:
    question_objs = questions["items"]
    by_key = {q.key: q for q in question_objs}
    missing = {q.key for q in question_objs} - {item.question_key for item in answers}
    if missing:
        raise HTTPException(status_code=422, detail=f"Missing answers for: {sorted(missing)}")

    normalized: list[OnboardingAnswerRequest] = []
    for item in answers:
        question = by_key.get(item.question_key)
        if not question:
            raise HTTPException(status_code=422, detail=f"Invalid question_key: {item.question_key}")

        valid_keys = {opt.key for opt in question.options}

        if question.type == "single_choice":
            if item.answer_key is None:
                raise HTTPException(status_code=422, detail=f"answer_key is required for question_key: {item.question_key}")
            if item.answer_key not in valid_keys:
                raise HTTPException(status_code=422, detail=f"Invalid answer_key for question_key: {item.question_key}")
            normalized.append(OnboardingAnswerRequest(question_key=item.question_key, answer_key=item.answer_key))

        else:
            selected = item.answer_keys if item.answer_keys is not None else (
                [item.answer_key] if item.answer_key is not None else None
            )
            if not selected:
                raise HTTPException(status_code=422, detail=f"answer_keys is required for question_key: {item.question_key}")
            deduped = list(dict.fromkeys(selected))
            invalid = [k for k in deduped if k not in valid_keys]
            if invalid:
                raise HTTPException(status_code=422, detail=f"Invalid answer_keys for question_key {item.question_key}: {invalid}")
            normalized.append(OnboardingAnswerRequest(question_key=item.question_key, answer_keys=deduped))

    return {"items": normalized, "detail": None}


@router.get("/onboarding/questions", response_model=OnboardingQuestionsResponse)
async def get_onboarding_questions(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
    lang: str = Query(default="pl"),
):
    catalog = _CATALOGS.get(lang, DEFAULT_ONBOARDING_CATALOG)
    questions = _build_questions(catalog)

    selected_answers = {
        "gender": current_user.gender,
        "interests": [],
    }

    result = await db.execute(select(UserPreferences).where(UserPreferences.user_id == current_user.id))
    user_prefs = result.scalar_one_or_none()
    if user_prefs and user_prefs.interests:
        selected_answers["interests"] = user_prefs.interests

    return OnboardingQuestionsResponse(
        items=questions["items"],
        selected_answers=OnboardingSelectedAnswers(**selected_answers),
    )


@router.post("/onboarding/answers", status_code=status.HTTP_204_NO_CONTENT, responses={422: {"model": ErrorResponse}})
async def save_onboarding_answers(
    body: OnboardingAnswersRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    answers = body.items
    if not answers:
        raise HTTPException(status_code=422, detail="Answers cannot be empty")

    questions = _build_questions(DEFAULT_ONBOARDING_CATALOG)
    normalized = _normalize_onboarding_answers(answers, questions)
    by_key = {item.question_key: item for item in normalized["items"]}

    current_user.gender = by_key["gender"].answer_key

    result = await db.execute(select(UserPreferences).where(UserPreferences.user_id == current_user.id))
    user_prefs = result.scalar()
    if not user_prefs:
        user_prefs = UserPreferences(user_id=current_user.id, interests=[])
        db.add(user_prefs)
    user_prefs.interests = by_key["interests"].answer_keys

    await db.commit()
