import hashlib
import secrets
from datetime import datetime, timedelta, timezone
from jose import JWTError, jwt
from passlib.context import CryptContext
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.models.models import RefreshToken, User
from app.schemas.schemas import TokenResponse

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class TokenService:

    def create_access_token(self, user_id: str) -> str:
        expire = datetime.now(timezone.utc) + timedelta(
            minutes=settings.JWT_ACCESS_TOKEN_EXPIRE_MINUTES
        )
        return jwt.encode(
            {"sub": user_id, "exp": expire, "type": "access"},
            settings.JWT_SECRET_KEY,
            algorithm=settings.JWT_ALGORITHM
        )

    def create_refresh_token(self) -> tuple[str, str]:
        raw = secrets.token_urlsafe(64)
        hashed = hashlib.sha256(raw.encode()).hexdigest()
        return raw, hashed

    def verify_access_token(self, token: str) -> str:
        try:
            payload = jwt.decode(
                token,
                settings.JWT_SECRET_KEY,
                algorithms=[settings.JWT_ALGORITHM]
            )
            if payload.get("type") != "access":
                raise ValueError("Invalid token type.")
            return payload["sub"]
        except JWTError:
            raise ValueError("Invalid token.")

    def hash_password(self, password: str) -> str:
        return pwd_context.hash(password[:72])

    def verify_password(self, plain: str, hashed: str) -> bool:
        return pwd_context.verify(plain[:72], hashed)

    def hash_refresh_token(self, raw: str) -> str:
        return hashlib.sha256(raw.encode()).hexdigest()

    async def issue_tokens(self, user: User, db: AsyncSession) -> TokenResponse:
        access_token = self.create_access_token(str(user.id))
        raw_refresh, hashed_refresh = self.create_refresh_token()

        result = await db.execute(
            select(RefreshToken)
            .where(RefreshToken.user_id == user.id, RefreshToken.revoked == False)
            .order_by(RefreshToken.expires_at.asc())
        )
        active_tokens = result.scalars().all()
        if len(active_tokens) >= settings.MAX_REFRESH_TOKENS_PER_USER:
            tokens_to_revoke = active_tokens[:len(active_tokens) - (settings.MAX_REFRESH_TOKENS_PER_USER - 1)]
            for t in tokens_to_revoke:
                t.revoked = True
            await db.commit()

        db.add(RefreshToken(
            user_id=user.id,
            token_hash=hashed_refresh,
            expires_at=datetime.now(timezone.utc) + timedelta(
                days=settings.JWT_REFRESH_TOKEN_EXPIRE_DAYS
            )
        ))
        await db.commit()

        return TokenResponse(access_token=access_token, refresh_token=raw_refresh)


token_service = TokenService()
