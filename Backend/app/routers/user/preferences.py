from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.config import DEFAULT_PREFERENCE_CATALOG
from app.core.database import get_db
from app.core.dependencies import get_current_user
from app.models.models import (
    PreferenceQuestionDefinition,
    User,
    UserPreferences,
)
from app.schemas.schemas import (
    PreferenceAnswerRequest,
    PreferenceAnswerResponse,
    PreferenceQuestion,
    PreferenceQuestionAnswerOption,
    UserPreferencesSchema,
)

router = APIRouter(prefix="/user", tags=["user"])


def _question_from_catalog_item(item: dict[str, object], question_id: int) -> PreferenceQuestion:
    answers = [
        PreferenceQuestionAnswerOption(
            answer_id=str(answer["answer_key"]),
            title=str(answer["title"]),
            body=answer.get("body") if isinstance(answer.get("body"), str) else None,
            trailingContent=answer.get("trailing_content") if isinstance(answer.get("trailing_content"), str) else None,
        )
        for answer in item.get("answers", [])
    ]
    return PreferenceQuestion(
        question_id=question_id,
        question_key=str(item["question_key"]),
        title=str(item["title"]),
        type=str(item["type"]),
        answers=answers,
    )


def _question_from_definition(question: PreferenceQuestionDefinition) -> PreferenceQuestion:
    return PreferenceQuestion(
        question_id=question.id,
        question_key=question.question_key,
        title=question.title,
        type=question.type,
        answers=[
            PreferenceQuestionAnswerOption(
                answer_id=answer.answer_key,
                title=answer.title,
                body=answer.body,
                trailingContent=answer.trailing_content,
            )
            for answer in question.answers
            if answer.is_active
        ],
    )


async def _preference_questions(db: AsyncSession) -> list[PreferenceQuestion]:
    result = await db.execute(
        select(PreferenceQuestionDefinition)
        .options(selectinload(PreferenceQuestionDefinition.answers))
        .order_by(PreferenceQuestionDefinition.sort_order.asc(), PreferenceQuestionDefinition.id.asc())
    )
    questions = result.scalars().all()
    if questions:
        return [_question_from_definition(question) for question in questions]
    return [
        _question_from_catalog_item(item, index + 1)
        for index, item in enumerate(DEFAULT_PREFERENCE_CATALOG)
    ]


def _normalize_preference_answers(
    answers: list[PreferenceAnswerRequest],
    questions: list[PreferenceQuestion],
) -> list[PreferenceAnswerResponse]:
    by_id = {question.question_id: question for question in questions}
    missing = {q.question_id for q in questions} - {item.question_id for item in answers}
    if missing:
        raise HTTPException(status_code=422, detail=f"Missing answers for question_ids: {sorted(missing)}")

    normalized: list[PreferenceAnswerResponse] = []
    for item in answers:
        question = by_id.get(item.question_id)
        if not question:
            raise HTTPException(status_code=422, detail=f"Invalid question_id: {item.question_id}")
        valid_answer_ids = {option.answer_id for option in question.answers}
        selected_ids = item.answer_ids if item.answer_ids is not None else (
            [item.answer_id] if item.answer_id is not None else None
        )
        if selected_ids is None:
            raise HTTPException(status_code=422, detail=f"answer_ids is required for question_id: {item.question_id}")
        deduped_ids = list(dict.fromkeys(selected_ids))
        if not deduped_ids:
            raise HTTPException(status_code=422, detail=f"answer_ids cannot be empty for question_id: {item.question_id}")
        invalid_ids = [answer_id for answer_id in deduped_ids if answer_id not in valid_answer_ids]
        if invalid_ids:
            raise HTTPException(status_code=422, detail=f"Invalid answer_ids for question_id {item.question_id}: {invalid_ids}")
        normalized.append(
            PreferenceAnswerResponse(question_id=item.question_id, answer_ids=deduped_ids)
        )
    return normalized


@router.get("/preferences/questions", response_model=list[PreferenceQuestion])
async def get_preferences_questions(db: AsyncSession = Depends(get_db)):
    return await _preference_questions(db)


@router.get("/preferences", response_model=UserPreferencesSchema)
async def get_preferences(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(select(UserPreferences).where(UserPreferences.user_id == current_user.id))
    user_prefs = result.scalar()
    if not user_prefs:
        raise HTTPException(status_code=404, detail="Preferences not found")
    return user_prefs


@router.post("/preferences/answers", response_model=list[PreferenceAnswerResponse])
async def save_preference_answers(
    answers: list[PreferenceAnswerRequest],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if not answers:
        raise HTTPException(status_code=422, detail="Answers cannot be empty")

    questions = await _preference_questions(db)
    normalized = _normalize_preference_answers(answers, questions)

    result = await db.execute(select(UserPreferences).where(UserPreferences.user_id == current_user.id))
    user_prefs = result.scalar()
    if not user_prefs:
        user_prefs = UserPreferences(user_id=current_user.id, interests=[])
        db.add(user_prefs)

    user_prefs.interests = [
        {"question_id": item.question_id, "answer_ids": item.answer_ids}
        for item in normalized
    ]

    await db.commit()
    return normalized
