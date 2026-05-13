from fastapi import Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.models.models import User 
from app.services.token_service import token_service
from app.core.redis import get_redis
from app.services.session_service import SessionService


security = HTTPBearer()

async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: AsyncSession = Depends(get_db)
) -> User:
    try:
        user_id = token_service.verify_access_token(credentials.credentials)
    except ValueError:
        raise HTTPException(status_code=401, detail="Invalid token.")

    user = await db.get(User, user_id)
    if not user:
        raise HTTPException(status_code=401, detail="User not found.")
    return user

async def get_current_admin(current_user: User = Depends(get_current_user)) -> User:
    if not current_user.is_admin:
        raise HTTPException(status_code=403, detail="Admin access required.")
    return current_user

def get_session_service() -> SessionService:
    return SessionService(get_redis())


async def get_valid_session(
    session_id: str,
    current_user: User = Depends(get_current_user),
    sessions: SessionService = Depends(get_session_service),
) -> str:
    valid = await sessions.verify(session_id, str(current_user.id))
    if not valid:
        raise HTTPException(403, detail="Session is invalid or expired.")
    return session_id