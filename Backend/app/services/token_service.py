import hashlib
import secrets
from datetime import datetime, timedelta, timezone
from jose import JWTError, jwt
from passlib.context import CryptContext
from app.core.config import settings

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
        print("HASH LEN:", len(password.encode()))
        return pwd_context.hash(password[:72])

    def verify_password(self, plain: str, hashed: str) -> bool:
        print("VERIFY LEN:", len(plain.encode()))
        return pwd_context.verify(plain[:72], hashed)

    def hash_refresh_token(self, raw: str) -> str:
        return hashlib.sha256(raw.encode()).hexdigest()


token_service = TokenService()