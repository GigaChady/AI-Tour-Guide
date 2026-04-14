from fastapi import APIRouter, Depends, HTTPException, WebSocket
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.dependencies import get_current_user
from app.core.redis import get_redis
from app.services.tour_stream import handle_tour_ws

router = APIRouter(prefix="/route", tags=["route"])


@router.websocket("/ws")
async def tour_ws(
    websocket: WebSocket,
    db: AsyncSession = Depends(get_db),
    redis=Depends(get_redis),
):
    await handle_tour_ws(websocket, db, redis)

#TODO: add endpoints for creating/editing/deleting routes, and for adding/removing POIs from routes. Also naming routes.

