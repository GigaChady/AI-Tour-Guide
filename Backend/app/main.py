import asyncio
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.core.audio_store import audio_store
from app.core.config import settings
from app.core.database import init_db
from app.core.redis import close_redis, init_redis
from app.routers import map as map_router
from app.routers import route
from app.routers.audio import router as audio_router
from app.routers.user import auth
from app.routers.user import demographics as user_demographics
from app.routers.user import preferences as user_preferences


async def _audio_cleanup_loop() -> None:
    while True:
        await asyncio.sleep(60)
        audio_store.cleanup_expired(settings.AUDIO_TTL_SECONDS)


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    await init_redis()
    cleanup_task = asyncio.create_task(_audio_cleanup_loop())
    yield
    cleanup_task.cancel()
    try:
        await cleanup_task
    except asyncio.CancelledError:
        pass
    await close_redis()

app = FastAPI(lifespan=lifespan, title="AI Tour Guide API", version="1.0.0")

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    first_error = exc.errors()[0]
    return JSONResponse(
        status_code=422,
        content={"detail": first_error["msg"]},
    )


app.include_router(auth.router)
app.include_router(route.router)
app.include_router(user_preferences.router)
app.include_router(user_demographics.router)
app.include_router(map_router.router)
app.include_router(audio_router)


@app.get("/health")
async def health_check():
    return {"status": "ok"}

@app.get("/ready")
async def readiness_check():
    return {"status": "ready"}  

@app.get("/live")
async def liveness_check():
    return {"status": "alive"}

@app.get("/version")
async def version():
    return {"version": app.version}
