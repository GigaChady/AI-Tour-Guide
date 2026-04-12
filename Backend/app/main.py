from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from app.core.database import init_db
from app.core.redis import init_redis, close_redis


from app.routers.user import auth
from app.routers import route
from app.routers.user import preferences as user_preferences
from app.routers.user import demographics as user_demographics
from app.routers import map as map_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    await init_redis()
    yield
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
