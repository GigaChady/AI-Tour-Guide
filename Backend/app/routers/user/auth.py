import logging
import re
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
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


def _validate_password_strength(password: str) -> None:
    if len(password) < 8:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail="Password must be at least 8 characters long")
    if not re.search(r"[A-Z]", password):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail="Password must contain at least one uppercase letter")
    if not re.search(r"[a-z]", password):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail="Password must contain at least one lowercase letter")
    if not re.search(r"\d", password):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail="Password must contain at least one digit")


def _mask_email_for_logs(email: str) -> str:
    #We want to mask email to reduce PII exposure in logs.
    local, sep, domain = email.partition("@")
    if not sep:
        return "***"
    if len(local) <= 1:
        return f"*{sep}{domain}"
    return f"{local[0]}***{sep}{domain}"


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
async def logout(body: LogoutRequest, db: AsyncSession = Depends(get_db)):
    token_hash = token_service.hash_refresh_token(body.refresh_token) 
    '''TUTAJ CHCEMY ODZNACZYĆ TOKEN JAKO UNIEWAŻNIONY, ABY NIE MÓGŁ BYĆ UŻYWANY DO ODNOWIENIA SESJI.
    NIE USUWAMY TOKENA Z BAZY, BO CHCEMY MIEĆ HISTORIĘ WYDANYCH TOKENÓW, NAWET TYCH UNIEWAŻNIONYCH.'''
    result = await db.execute( 
        select(RefreshToken).where(
            RefreshToken.token_hash == token_hash,
            RefreshToken.revoked.is_(False), # Nie używamy == False, bo może być NULL, a wtedy porównanie zwróci NULL zamiast False
            RefreshToken.expires_at > datetime.now(timezone.utc) # Nie używamy > datetime.now(), bo może być NULL, a wtedy porównanie zwróci NULL zamiast False
        )
    )
    stored = result.scalar()
    if stored:
        stored.revoked = True
        await db.commit()

        # Logowanie wycofania tokena dla audytu
        logger.info(f"Refresh token revoked for user_id: {stored.user_id} at {datetime.now(timezone.utc).isoformat()}")
        # indempotent logout: even if token is already revoked or expired, we return 204 to avoid revealing token validity
    return


@router.post("/register", response_model=TokenResponse) 
async def register(body: RegisterRequest, db: AsyncSession = Depends(get_db)):
    normalized_email = body.email.strip().lower() # normalizing email to prevent duplicates due to case/whitespace differences

    _validate_password_strength(body.password) # password validation 

    if not getattr(body, "name", None):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail="Name is required")

    existing = await db.execute(select(User).where(User.email == normalized_email))
    if existing.scalar():
        raise HTTPException(status.HTTP_409_CONFLICT, detail="Email already in use") # If u know u know

    user = User(
        email=normalized_email,
        hashed_password=token_service.hash_password(body.password),
        imie=getattr(body, "imie", None) or getattr(body, "name", None),
        nazwisko=getattr(body, "nazwisko", None),
    )
    db.add(user)

    try: # race condition handling: if two requests try to register the same email at the same time, one will succeed and the other will raise an IntegrityError due to unique constraint violation. We catch that and return a 409 conflict instead of a 500 server error.
        await db.commit()
    except IntegrityError:
        await db.rollback()
        raise HTTPException(status.HTTP_409_CONFLICT, detail="Email already in use")

    await db.refresh(user)

    # Logowanie rejestracji użytkownika dla audytu
    logger.info(f"User registered successfully for email: {normalized_email}")

    return await token_service.issue_tokens(user, db)


@router.post("/login", response_model=TokenResponse)
async def login(body: LoginRequest, db: AsyncSession = Depends(get_db)):
    normalized_email = body.email.strip().lower() # same as in register 
    masked_email = _mask_email_for_logs(normalized_email) # def upper 

    result = await db.execute(select(User).where(User.email == normalized_email))
    user = result.scalar()

    if not user or not token_service.verify_password(body.password, user.hashed_password): # to reduce account enumeration risk
        logger.warning(f"Failed login attempt for email: {masked_email}")
        raise HTTPException(401, detail="Invalid email or password")

    # Logging
    logger.info(f"User logged in successfully for email: {masked_email}")

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
        raise HTTPException(401, detail="Invalid Google token")

    issuer = user_info.get("iss")
    if not user_info.get("email_verified") or issuer not in {"accounts.google.com", "https://accounts.google.com"}:
        logger.warning("Rejected Google token due to unverified email or invalid issuer")
        raise HTTPException(401, detail="Invalid Google token")

    google_sub = user_info["sub"]
    normalized_email = user_info["email"].strip().lower()
    masked_email = _mask_email_for_logs(normalized_email)

    result = await db.execute(select(User).where(User.google_id == google_sub))
    user = result.scalar()

    if not user:
        result_by_email = await db.execute(select(User).where(User.email == normalized_email))
        user_by_email = result_by_email.scalar()

        if user_by_email:
            if not user_by_email.google_id:
                user_by_email.google_id = google_sub
                user = user_by_email
            elif user_by_email.google_id != google_sub:
                logger.warning(f"Google account conflict for email: {masked_email}")
                raise HTTPException(status.HTTP_409_CONFLICT, detail="Email already linked to another Google account")
            else:
                user = user_by_email

    if not user:
        user = User(
            email=normalized_email,
            google_id=google_sub,
            imie=user_info.get("given_name"),
            nazwisko=user_info.get("family_name"),
        )
        db.add(user)

    try:
        await db.commit()
    except IntegrityError:
        await db.rollback()
        logger.warning(f"Google auth race/conflict for email: {masked_email}")
        raise HTTPException(status.HTTP_409_CONFLICT, detail="Account conflict during Google login")

    await db.refresh(user)
    logger.info(f"Google login successful for email: {masked_email}")

    return await token_service.issue_tokens(user, db)


@router.post("/refresh", response_model=TokenResponse)
async def refresh(body: RefreshRequest, db: AsyncSession = Depends(get_db)):
    token_hash = token_service.hash_refresh_token(body.refresh_token)

    result = await db.execute(
        select(RefreshToken)
        .where(
            RefreshToken.token_hash == token_hash,
            RefreshToken.revoked.is_(False),
            RefreshToken.expires_at > datetime.now(timezone.utc)
        )
        .with_for_update()
    )
    stored = result.scalar()

    if not stored:
        logger.warning("Invalid or expired refresh token used")
        raise HTTPException(401, detail="Token expired or invalid")

    stored.revoked = True

    user = await db.get(User, stored.user_id)
    if not user:
        await db.rollback()
        logger.warning("Refresh token points to missing user")
        raise HTTPException(401, detail="Token expired or invalid")

    await db.commit()
    logger.info(f"Refresh token rotated successfully for user_id: {user.id}")
    return await token_service.issue_tokens(user, db)
