
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
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
    PreferenceQuestionInput,
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

    input_schema = None
    if item.get("type") == "percentage":
        input_schema = PreferenceQuestionInput(
            min=int(item.get("min_value", 0)),
            max=int(item.get("max_value", 100)),
            required=bool(item.get("required", False)),
        )

    return PreferenceQuestion(
        question_id=question_id,
        question_key=str(item["question_key"]),
        title=str(item["title"]),
        type=str(item["type"]),
        answers=answers,
        input=input_schema,
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
        input=PreferenceQuestionInput(
            min=question.min_value,
            max=question.max_value,
            required=question.required,
        ) if question.type == "percentage" else None,
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
    normalized: list[PreferenceAnswerResponse] = []

    for item in answers:
        question = by_id.get(item.question_id)
        if not question:
            raise HTTPException(status_code=422, detail=f"Invalid question_id: {item.question_id}")

        if question.type == "percentage":
            if item.value is None:
                raise HTTPException(status_code=422, detail=f"value is required for question_id: {item.question_id}")
            if item.value < 0 or item.value > 100:
                raise HTTPException(status_code=422, detail=f"value must be between 0 and 100 for question_id: {item.question_id}")
            if item.answer_id is not None or item.answer_ids is not None:
                raise HTTPException(status_code=422, detail=f"Only value is allowed for question_id: {item.question_id}")

            normalized.append(
                PreferenceAnswerResponse(
                    question_id=item.question_id,
                    value=item.value,
                )
            )
            continue

        valid_answer_ids = {option.answer_id for option in question.answers}
        if question.type == "multi_choice":
            selected_ids = item.answer_ids
            if selected_ids is None:
                if item.answer_id is not None:
                    selected_ids = [item.answer_id]
                else:
                    raise HTTPException(status_code=422, detail=f"answer_ids is required for question_id: {item.question_id}")
            deduped_ids = list(dict.fromkeys(selected_ids))
            if not deduped_ids:
                raise HTTPException(status_code=422, detail=f"answer_ids cannot be empty for question_id: {item.question_id}")
            invalid_ids = [answer_id for answer_id in deduped_ids if answer_id not in valid_answer_ids]
            if invalid_ids:
                raise HTTPException(status_code=422, detail=f"Invalid answer_ids for question_id {item.question_id}: {invalid_ids}")
            if item.value is not None:
                raise HTTPException(status_code=422, detail=f"Only answer_ids is allowed for question_id: {item.question_id}")

            normalized.append(
                PreferenceAnswerResponse(
                    question_id=item.question_id,
                    answer_ids=deduped_ids,
                )
            )
            continue

        if item.answer_id is None:
            raise HTTPException(status_code=422, detail=f"answer_id is required for question_id: {item.question_id}")
        if item.answer_id not in valid_answer_ids:
            raise HTTPException(status_code=422, detail=f"Invalid answer_id for question_id {item.question_id}: {item.answer_id}")
        if item.answer_ids is not None or item.value is not None:
            raise HTTPException(status_code=422, detail=f"Only answer_id is allowed for question_id: {item.question_id}")

        normalized.append(
            PreferenceAnswerResponse(
                question_id=item.question_id,
                answer_id=item.answer_id,
            )
        )

    return normalized


@router.get("/preferences/questions", response_model=list[PreferenceQuestion])
async def get_preferences_questions(db: AsyncSession = Depends(get_db)):
    return await _preference_questions(db)

@router.get("/preferences", response_model=UserPreferencesSchema)
async def get_preferences(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
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
    normalized_answers = _normalize_preference_answers(answers, questions)

    result = await db.execute(select(UserPreferences).where(UserPreferences.user_id == current_user.id))
    user_prefs = result.scalar()
    if not user_prefs:
        user_prefs = UserPreferences(user_id=current_user.id, interests=[])
        db.add(user_prefs)

    serialized_answers = []
    for item in normalized_answers:
        payload: dict[str, object] = {"question_id": item.question_id}
        if item.value is not None:
            payload["value"] = item.value
        elif item.answer_ids is not None:
            payload["answer_ids"] = item.answer_ids
        elif item.answer_id is not None:
            payload["answer_id"] = item.answer_id
        serialized_answers.append(payload)

    user_prefs.interests = serialized_answers

    await db.commit()
    return normalized_answers
