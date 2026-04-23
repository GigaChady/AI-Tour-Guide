from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from app.core.database import get_db
from app.models.models import User
from app.schemas.schemas import UpdateUserParamsRequest, ErrorResponse, UserParamsResponse

from app.routers.user.auth import _validate_password_strength
from app.services.token_service import token_service
from app.core.dependencies import get_current_user

router = APIRouter(prefix="/user", tags=["user"])


@router.patch("/update-params", status_code=status.HTTP_204_NO_CONTENT, responses={400: {"model": ErrorResponse}, 409: {"model": ErrorResponse}})
async def update_user_params(
    body: UpdateUserParamsRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if body.new_password is not None:
        _validate_password_strength(body.new_password)
        current_user.hashed_password = token_service.hash_password(body.new_password)

    if body.new_email is not None:
        normalized_email = body.new_email.strip().lower()
        if normalized_email == current_user.email:
            raise HTTPException(status.HTTP_400_BAD_REQUEST, detail="New email must be different")
        existing = await db.execute(select(User).where(User.email == normalized_email))
        if existing.scalar():
            raise HTTPException(status.HTTP_409_CONFLICT, detail="Email already in use")
        current_user.email = normalized_email

    if body.name is not None:
        if not body.name:
            raise HTTPException(status.HTTP_400_BAD_REQUEST, detail="Name is required")
        current_user.name = body.name

    try:
        await db.commit()
    except IntegrityError:
        await db.rollback()
        raise HTTPException(status.HTTP_409_CONFLICT, detail="Email already in use")
    return


@router.get("/me-params", response_model=UserParamsResponse)
async def get_current_user_params(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(select(User).where(User.id == current_user.id))
    user_from_db = result.scalar_one()
    return UserParamsResponse(
        email=user_from_db.email,
        name=user_from_db.name,
    )


