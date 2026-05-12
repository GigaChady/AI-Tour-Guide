import logging
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, delete
from sqlalchemy.exc import IntegrityError

from app.core.database import get_db
from app.models.models import User, UserPreferences, UserNarrationSettings, Route, RoutePoi, RefreshToken
from app.schemas.schemas import RegisterRequest, ErrorResponse, UserParamsResponse, AdminUpdateUserRequest
from app.services.token_service import token_service
from app.core.dependencies import get_current_user, get_current_admin
from app.routers.user.auth import _validate_password_strength

logger = logging.getLogger("user_crud")

router = APIRouter(prefix="/user", tags=["user"])


@router.post("/add", status_code=status.HTTP_201_CREATED, responses={400: {"model": ErrorResponse}, 409: {"model": ErrorResponse}})
async def create_user(
    body: RegisterRequest,
    db: AsyncSession = Depends(get_db),
    _admin: User = Depends(get_current_admin),
):
    normalized_email = body.email.strip().lower()
    _validate_password_strength(body.password)

    existing = await db.execute(select(User).where(User.email == normalized_email))
    if existing.scalar():
        raise HTTPException(status.HTTP_409_CONFLICT, detail="Email already in use")

    user = User(
        email=normalized_email,
        hashed_password=token_service.hash_password(body.password),
        name=body.name,
    )
    db.add(user)
    try:
        await db.commit()
    except IntegrityError:
        await db.rollback()
        raise HTTPException(status.HTTP_409_CONFLICT, detail="Email already in use")

    return


@router.delete(
    "/delete/{user_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    responses={403: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
async def delete_user(
    user_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if not current_user.is_admin and current_user.id != user_id:
        raise HTTPException(status.HTTP_403_FORBIDDEN, detail="Not authorized")

    target = await db.get(User, user_id)
    if not target:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="User not found")

    route_ids_result = await db.execute(select(Route.id).where(Route.user_id == user_id))
    route_ids = [r for (r,) in route_ids_result.all()]
    if route_ids:
        await db.execute(delete(RoutePoi).where(RoutePoi.route_id.in_(route_ids)))
    await db.execute(delete(Route).where(Route.user_id == user_id))
    await db.execute(delete(UserPreferences).where(UserPreferences.user_id == user_id))
    await db.execute(delete(UserNarrationSettings).where(UserNarrationSettings.user_id == user_id))
    await db.execute(delete(RefreshToken).where(RefreshToken.user_id == user_id))
    await db.delete(target)
    await db.commit()
    return



@router.get(
    "/{user_id}",
    response_model=UserParamsResponse,
    responses={403: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
async def get_user(
    user_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if not current_user.is_admin and current_user.id != user_id:
        raise HTTPException(status.HTTP_403_FORBIDDEN, detail="Not authorized")

    target = await db.get(User, user_id)
    if not target:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="User not found")

    return UserParamsResponse(email=target.email, name=target.name)


@router.patch(
    "/{user_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    responses={400: {"model": ErrorResponse}, 403: {"model": ErrorResponse}, 404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}},
)
async def update_user(
    user_id: UUID,
    body: AdminUpdateUserRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if not current_user.is_admin and current_user.id != user_id:
        raise HTTPException(status.HTTP_403_FORBIDDEN, detail="Not authorized")

    target = await db.get(User, user_id)
    if not target:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="User not found")

    if body.new_password is not None:
        _validate_password_strength(body.new_password)
        target.hashed_password = token_service.hash_password(body.new_password)

    if body.new_email is not None:
        normalized_email = body.new_email.strip().lower()
        if normalized_email == target.email:
            raise HTTPException(status.HTTP_400_BAD_REQUEST, detail="New email must be different")
        existing = await db.execute(select(User).where(User.email == normalized_email))
        if existing.scalar():
            raise HTTPException(status.HTTP_409_CONFLICT, detail="Email already in use")
        target.email = normalized_email

    if body.name is not None:
        if not body.name.strip():
            raise HTTPException(status.HTTP_400_BAD_REQUEST, detail="Name cannot be empty")
        target.name = body.name

    try:
        await db.commit()
    except IntegrityError:
        await db.rollback()
        raise HTTPException(status.HTTP_409_CONFLICT, detail="Email already in use")
    return

