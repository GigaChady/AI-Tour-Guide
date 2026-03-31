from contextlib import asynccontextmanager
from fastapi import FastAPI
from app.core.database import init_db
from app.core.redis import init_redis, close_redis


from app.routers import auth, route, narration
from app.routers.user import preferences as user_preferences
from app.routers import map as map_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    await init_redis()
    yield
    await close_redis()

# from app.core.database import init_db_and_session
# @asynccontextmanager
# async def lifespan(app: FastAPI):
#     # initialize DB + session tied to this loop
#     await init_db_and_session(app)
#     # optionally initialize Redis here
#     yield
#     # cleanup DB engine
#     await app.state.engine.dispose()

app = FastAPI(lifespan=lifespan, title="AI Tour Guide API", version="1.0.0")


app.include_router(auth.router)
app.include_router(route.route)
app.include_router(narration.router)
app.include_router(user_preferences.router)
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
