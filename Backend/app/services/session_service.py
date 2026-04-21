import uuid

from app.schemas.schemas import SessionMeta


class SessionService:
    def __init__(self, redis_client):
        self.redis = redis_client

    async def create(self, user_id: str, route_id: str) -> str:
        session_id = str(uuid.uuid4())
        await self.redis.set(
            f"session:{session_id}:meta",
            SessionMeta(user_id=user_id, route_id=route_id).model_dump_json()
        )
        return session_id

    async def end_session(self, session_id: str):
        await self.redis.delete(
            f"session:{session_id}:meta",
            f"session:{session_id}:pois",
            f"session:{session_id}:visited",
        )

    async def get_meta(self, session_id: str) -> SessionMeta | None:
        meta = await self.redis.get(f"session:{session_id}:meta")
        if meta:
            return SessionMeta.model_validate_json(meta)
        return None

    async def verify(self, session_id: str, user_id: str) -> bool:
        meta = await self.get_meta(session_id)
        if not meta:
            return False
        return meta.user_id == user_id
