from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.database import get_db
from app.core.dependencies import get_current_user
from app.models.models import DemographicsGenderOption, User
from app.schemas.schemas import (
    DemographicsAnswerRequest,
    DemographicsQuestion,
    DemographicsQuestionAnswerOption,
)

router = APIRouter(prefix="/user", tags=["user"])


def _gender_title(value: str) -> str:
    return value.replace("_", " ").title()


def _gender_ids_from_settings() -> list[str]:
    values = [item.strip() for item in settings.DEMOGRAPHICS_GENDER_OPTIONS.split(",")]
    return [item for item in values if item]


def _gender_options_from_settings() -> list[DemographicsQuestionAnswerOption]:
    return [
        DemographicsQuestionAnswerOption(answer_id=option, title=_gender_title(option))
        for option in _gender_ids_from_settings()
    ]


async def _gender_options_from_db(db: AsyncSession) -> list[DemographicsQuestionAnswerOption]:
    result = await db.execute(
        select(DemographicsGenderOption)
        .where(DemographicsGenderOption.is_active == True)
        .order_by(DemographicsGenderOption.sort_order.asc(), DemographicsGenderOption.id.asc())
    )
    rows = result.scalars().all()
    return [
        DemographicsQuestionAnswerOption(answer_id=row.code, title=row.label)
        for row in rows
    ]


async def _gender_options_with_fallback(db: AsyncSession) -> list[DemographicsQuestionAnswerOption]:
    options = await _gender_options_from_db(db)
    if not options:
        return _gender_options_from_settings()
    return options


async def _demographics_questions(db: AsyncSession) -> list[DemographicsQuestion]:
    return [
        DemographicsQuestion(
            question_key="gender",
            title="Jaka jest Twoja płeć?",
            type="single_choice",
            answers=await _gender_options_with_fallback(db),
        ),
    ]


def _normalize_demographics_answers(
    answers: list[DemographicsAnswerRequest],
    questions: list[DemographicsQuestion],
) -> list[DemographicsAnswerRequest]:
    by_key = {q.question_key: q for q in questions}
    missing = {q.question_key for q in questions} - {item.question_key for item in answers}
    if missing:
        raise HTTPException(status_code=422, detail=f"Missing answers for: {sorted(missing)}")

    normalized: list[DemographicsAnswerRequest] = []
    for item in answers:
        question = by_key.get(item.question_key)
        if not question:
            raise HTTPException(status_code=422, detail=f"Invalid question_key: {item.question_key}")
        valid_ids = {opt.answer_id for opt in question.answers}
        if item.answer_id not in valid_ids:
            raise HTTPException(status_code=422, detail=f"Invalid answer_id for question_key: {item.question_key}")
        normalized.append(item)
    return normalized


@router.get("/demographics/questions", response_model=list[DemographicsQuestion])
async def get_demographics_questions(db: AsyncSession = Depends(get_db)):
    return await _demographics_questions(db)


@router.post("/demographics/answers", status_code=status.HTTP_204_NO_CONTENT)
async def save_demographics_answers(
    answers: list[DemographicsAnswerRequest],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if not answers:
        raise HTTPException(status_code=422, detail="Answers cannot be empty")

    questions = await _demographics_questions(db)
    normalized = _normalize_demographics_answers(answers, questions)

    by_key = {item.question_key: item for item in normalized}
    gender_answer = by_key["gender"]

    selected_code = str(gender_answer.answer_id)
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

    await db.commit()
