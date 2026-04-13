from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.config import DEFAULT_ONBOARDING_CATALOG
from app.core.database import get_db
from app.core.dependencies import get_current_user
from app.models.models import DemographicsGenderOption, PreferenceQuestionDefinition, User, UserPreferences
from app.schemas.schemas import (
    OnboardingAnswerRequest,
    OnboardingAnswerResponse,
    OnboardingOption,
    OnboardingQuestion,
)

router = APIRouter(prefix="/user", tags=["user"])


def _options_from_catalog(question_key: str) -> list[OnboardingOption]:
    item = next((i for i in DEFAULT_ONBOARDING_CATALOG if i["question_key"] == question_key), None)
    if not item:
        return []
    return [
        OnboardingOption(
            key=str(a["answer_key"]),
            title=str(a["title"]),
            body=a.get("body") if isinstance(a.get("body"), str) else None,
            trailing_content=a.get("trailing_content") if isinstance(a.get("trailing_content"), str) else None,
        )
        for a in item.get("answers", [])
    ]


async def _gender_options(db: AsyncSession) -> list[OnboardingOption]:
    result = await db.execute(
        select(DemographicsGenderOption)
        .where(DemographicsGenderOption.is_active == True)
        .order_by(DemographicsGenderOption.sort_order.asc(), DemographicsGenderOption.id.asc())
    )
    rows = result.scalars().all()
    if rows:
        return [OnboardingOption(key=row.code, title=row.label) for row in rows]
    return _options_from_catalog("gender")


async def _interests_options(db: AsyncSession) -> list[OnboardingOption]:
    result = await db.execute(
        select(PreferenceQuestionDefinition)
        .options(selectinload(PreferenceQuestionDefinition.answers))
        .where(PreferenceQuestionDefinition.question_key == "interests")
    )
    question = result.scalar_one_or_none()
    if question:
        return [
            OnboardingOption(key=a.answer_key, title=a.title, body=a.body, trailing_content=a.trailing_content)
            for a in question.answers if a.is_active
        ]
    return _options_from_catalog("interests")



def _build_questions(
    gender_opts: list[OnboardingOption],
    interests_opts: list[OnboardingOption],
) -> list[OnboardingQuestion]:
    gender_catalog = next(i for i in DEFAULT_ONBOARDING_CATALOG if i["question_key"] == "gender")
    interests_catalog = next(i for i in DEFAULT_ONBOARDING_CATALOG if i["question_key"] == "interests")
    return [
        OnboardingQuestion(key="gender", title=gender_catalog["title"], type="single_choice", options=gender_opts),
        OnboardingQuestion(key="interests", title=interests_catalog["title"], type="multi_choice", options=interests_opts),
    ]



def _normalize_onboarding_answers(
    answers: list[OnboardingAnswerRequest],
    questions: list[OnboardingQuestion],
) -> list[OnboardingAnswerResponse]:
    by_key = {q.key: q for q in questions}
    missing = {q.key for q in questions} - {item.question_key for item in answers}
    if missing:
        raise HTTPException(status_code=422, detail=f"Missing answers for: {sorted(missing)}")

    normalized: list[OnboardingAnswerResponse] = []
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
            normalized.append(OnboardingAnswerResponse(question_key=item.question_key, answer_key=item.answer_key))

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
            normalized.append(OnboardingAnswerResponse(question_key=item.question_key, answer_keys=deduped))

    return normalized



@router.get("/onboarding/questions", response_model=list[OnboardingQuestion])
async def get_onboarding_questions(db: AsyncSession = Depends(get_db)):
    return _build_questions(
        gender_opts=await _gender_options(db),
        interests_opts=await _interests_options(db),
    )


@router.post("/onboarding/answers", response_model=list[OnboardingAnswerResponse])
async def save_onboarding_answers(
    answers: list[OnboardingAnswerRequest],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if not answers:
        raise HTTPException(status_code=422, detail="Answers cannot be empty")

    questions = _build_questions(
        gender_opts=await _gender_options(db),
        interests_opts=await _interests_options(db),
    )
    normalized = _normalize_onboarding_answers(answers, questions)

    by_key = {item.question_key: item for item in normalized}

    gender_answer = by_key["gender"]
    selected_code = str(gender_answer.answer_key)
    option_result = await db.execute(
        select(DemographicsGenderOption).where(
            DemographicsGenderOption.code == selected_code,
            DemographicsGenderOption.is_active == True,
        )
    )
    option = option_result.scalar_one_or_none()
    if option:
        current_user.gender_option_id = option.id
        current_user.gender_custom = None
    else:
        current_user.gender_option_id = None
        current_user.gender_custom = selected_code

    result = await db.execute(select(UserPreferences).where(UserPreferences.user_id == current_user.id))
    user_prefs = result.scalar()
    if not user_prefs:
        user_prefs = UserPreferences(user_id=current_user.id, interests=[])
        db.add(user_prefs)
    user_prefs.interests = [{"answer_keys": by_key["interests"].answer_keys}]

    await db.commit()
    return normalized
