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
    DemographicsQuestionInput,
)

router = APIRouter(prefix="/user", tags=["user"])


def _gender_title(value: str) -> str:
    return value.replace("_", " ").title()


def _gender_ids_from_settings() -> list[str]:
    values = [item.strip() for item in settings.DEMOGRAPHICS_GENDER_OPTIONS.split(",")]
    return [item for item in values if item]


def _gender_options_from_settings() -> list[DemographicsQuestionAnswerOption]:
    gender_ids = _gender_ids_from_settings()
    options = [
        DemographicsQuestionAnswerOption(
            answer_id=option,
            title=_gender_title(option),
        )
        for option in gender_ids
    ]
    options.append(
        DemographicsQuestionAnswerOption(
            answer_id=settings.DEMOGRAPHICS_CUSTOM_GENDER_ANSWER_ID,
            title="Inna",
            body="Wpisz własną wartość",
        )
    )
    return options


async def _gender_options_from_db(db: AsyncSession) -> list[DemographicsQuestionAnswerOption]:
    result = await db.execute(
        select(DemographicsGenderOption)
        .where(DemographicsGenderOption.is_active == True)
        .order_by(DemographicsGenderOption.sort_order.asc(), DemographicsGenderOption.id.asc())
    )
    rows = result.scalars().all()

    options = [
        DemographicsQuestionAnswerOption(
            answer_id=row.code,
            title=row.label,
        )
        for row in rows
    ]
    return options


async def _gender_options_with_fallback(db: AsyncSession) -> list[DemographicsQuestionAnswerOption]:
    options = await _gender_options_from_db(db)
    if not options:
        options = _gender_options_from_settings()
        return options

    options.append(
        DemographicsQuestionAnswerOption(
            answer_id=settings.DEMOGRAPHICS_CUSTOM_GENDER_ANSWER_ID,
            title="Inna",
            body="Wpisz własną wartość",
        )
    )
    return options


@router.get("/demographics/questions", response_model=list[DemographicsQuestion])
async def get_demographics_questions(db: AsyncSession = Depends(get_db)):
    return [
        DemographicsQuestion(
            question_key="gender",
            title="Jaka jest Twoja płeć?",
            type="single_choice",
            answers=await _gender_options_with_fallback(db),
            allow_custom_text=True,
            custom_text_key="gender_custom",
        ),
        DemographicsQuestion(
            question_key="age",
            title="Ile masz lat?",
            type="number_input",
            answers=[],
            input=DemographicsQuestionInput(
                value_key="age_value",
                min=settings.DEMOGRAPHICS_MIN_AGE,
                max=settings.DEMOGRAPHICS_MAX_AGE,
                required=True,
            ),
        ),
    ]


@router.post("/demographics/answers", status_code=status.HTTP_204_NO_CONTENT)
async def save_demographics_answers(
    answers: list[DemographicsAnswerRequest],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    by_key = {item.question_key: item for item in answers}

    gender_answer = by_key.get("gender")
    age_answer = by_key.get("age")

    if not gender_answer:
        raise HTTPException(status_code=422, detail="Missing gender answer")
    if not age_answer:
        raise HTTPException(status_code=422, detail="Missing age answer")

    current_gender_options = await _gender_options_with_fallback(db)
    valid_gender_ids = {item.answer_id for item in current_gender_options if item.answer_id != settings.DEMOGRAPHICS_CUSTOM_GENDER_ANSWER_ID}
    if gender_answer.answer_id == settings.DEMOGRAPHICS_CUSTOM_GENDER_ANSWER_ID:
        if not gender_answer.custom_text or not gender_answer.custom_text.strip():
            raise HTTPException(status_code=422, detail="custom_text is required for custom gender")
        current_user.gender_option_id = None
        current_user.gender_custom = gender_answer.custom_text.strip()
    elif gender_answer.answer_id in valid_gender_ids:
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
            # Fallback path when options come from env and are not seeded in DB yet.
            current_user.gender_option_id = None
            current_user.gender_custom = selected_code
    else:
        raise HTTPException(status_code=422, detail="Invalid gender answer_id")

    if age_answer.value is None:
        raise HTTPException(status_code=422, detail="value is required for age")
    if age_answer.value < settings.DEMOGRAPHICS_MIN_AGE or age_answer.value > settings.DEMOGRAPHICS_MAX_AGE:
        raise HTTPException(status_code=422, detail=f"Age must be between {settings.DEMOGRAPHICS_MIN_AGE} and {settings.DEMOGRAPHICS_MAX_AGE}")

    current_user.wiek = float(age_answer.value)

    await db.commit()
    return
