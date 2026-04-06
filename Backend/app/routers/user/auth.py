import logging
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from google.oauth2 import id_token
from google.auth.transport import requests as google_requests

from app.core.database import get_db
from app.core.config import settings
from app.models.models import User, RefreshToken
from app.schemas.schemas import (
    RegisterRequest, LoginRequest, GoogleAuthRequest,
    TokenResponse, RefreshRequest, LogoutRequest
)
from app.services.token_service import token_service

logger = logging.getLogger("auth")

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
async def logout(body: LogoutRequest, db: AsyncSession = Depends(get_db)):
    token_hash = token_service.hash_refresh_token(body.refresh_token)
    result = await db.execute(
        select(RefreshToken).where(
            RefreshToken.token_hash == token_hash,
            RefreshToken.revoked == False,
            RefreshToken.expires_at > datetime.now(timezone.utc)
        )
    )
    stored = result.scalar()
    if not stored:
        logger.warning("Invalid or expired refresh token used for logout")
        raise HTTPException(401, "Token expired or invalid")
    stored.revoked = True
    await db.commit()
    return


@router.post("/register", response_model=TokenResponse)
async def register(body: RegisterRequest, db: AsyncSession = Depends(get_db)):
    existing = await db.execute(select(User).where(User.email == body.email))
    if existing.scalar():
        raise HTTPException(400, "Email already in use")

    user = User(
        email=body.email,
        hashed_password=token_service.hash_password(body.password),
        imie=getattr(body, "imie", None),
        nazwisko=getattr(body, "nazwisko", None),
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)

    return await token_service.issue_tokens(user, db)


@router.post("/login", response_model=TokenResponse)
async def login(body: LoginRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).where(User.email == body.email))
    user = result.scalar()

    if not user or not token_service.verify_password(body.password, user.hashed_password):
        logger.warning(f"Failed login attempt for email: {body.email}")
        raise HTTPException(401, "Invalid email or password")

    return await token_service.issue_tokens(user, db)


@router.post("/google", response_model=TokenResponse)
async def google_auth(body: GoogleAuthRequest, db: AsyncSession = Depends(get_db)):
    try:
        user_info = id_token.verify_oauth2_token(
            body.google_token,
            google_requests.Request(),
            settings.GOOGLE_CLIENT_ID
        )
    except ValueError:
        logger.warning("Invalid Google token during login attempt")
        raise HTTPException(401, "Invalid Google token")

    result = await db.execute(
        select(User).where(User.google_id == user_info["sub"])
    )
    user = result.scalar()

    if not user:
        user = User(
            email=user_info["email"],
            google_id=user_info["sub"],
            imie=user_info.get("given_name"),
            nazwisko=user_info.get("family_name"),
        )
        db.add(user)
        await db.commit()
        await db.refresh(user)

    return await token_service.issue_tokens(user, db)


@router.post("/refresh", response_model=TokenResponse)
async def refresh(body: RefreshRequest, db: AsyncSession = Depends(get_db)):
    token_hash = token_service.hash_refresh_token(body.refresh_token)

    result = await db.execute(
        select(RefreshToken).where(
            RefreshToken.token_hash == token_hash,
            RefreshToken.revoked == False,
            RefreshToken.expires_at > datetime.now(timezone.utc)
        )
    )
    stored = result.scalar()

    if not stored:
        logger.warning("Invalid or expired refresh token used")
        raise HTTPException(401, "Token expired or invalid")

    stored.revoked = True
    await db.commit()

    user = await db.get(User, stored.user_id)
    return await token_service.issue_tokens(user, db)
