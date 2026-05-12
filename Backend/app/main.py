from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from sqlalchemy import select
from app.core.database import init_db, AsyncSessionLocal
from app.core.redis import close_redis, init_redis
from app.models.models import SystemConfig
from app.services.tts.factory import set_tts_provider
from app.routers import map as map_router
from app.routers import route
from app.routers.dashboard import router as dashboard_router
from app.routers.web_history import router as web_history_router
from app.routers.web_admin import router as web_admin_router
from app.routers.user import auth
from app.routers.user import narration_settings as user_narration_settings
from app.routers.user import onboarding as user_onboarding
from app.routers.user import login_settings as user_login_settings
from app.routers.user import user_crud as user_crud_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    await init_redis()
    async with AsyncSessionLocal() as db:
        result = await db.execute(select(SystemConfig).where(SystemConfig.key == "tts_provider"))
        row = result.scalar_one_or_none()
        if row:
            set_tts_provider(row.value)
    yield
    await close_redis()


app = FastAPI(lifespan=lifespan, title="AI Tour Guide API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    first_error = exc.errors()[0]
    return JSONResponse(
        status_code=422,
        content={"detail": first_error["msg"]},
    )


app.include_router(auth.router)
app.include_router(route.router)
app.include_router(dashboard_router)
app.include_router(user_onboarding.router)
app.include_router(user_narration_settings.router)
app.include_router(user_login_settings.router)
app.include_router(user_crud_router.router)
app.include_router(map_router.router)
app.include_router(web_history_router)
app.include_router(web_admin_router)


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
