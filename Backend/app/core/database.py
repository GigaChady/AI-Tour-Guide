from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker
from app.core.config import settings
from app.models import User, UserPreferences, RefreshToken, Base
from sqlalchemy import text

engine = create_async_engine(settings.DATABASE_URL, echo=False)

AsyncSessionLocal = async_sessionmaker(
    engine,
    class_=AsyncSession,
    expire_on_commit=False
)



async def init_db(): # na produkcji trzeba bedzie to wywolywac przy starcie aplikacji zeby stworzyc tabele w bazie danych, na razie mozna to robic recznie albo przez migracje ale to juz pozniej
    # mozna potem uzyc alembic do migracji ale na razie niech zostanie tak jak jest zeby bylo szybciej
    async with engine.begin() as conn:
        await conn.execution_options(isolation_level="AUTOCOMMIT")
        await conn.execute(text("CREATE EXTENSION IF NOT EXISTS postgis")) # dodajemy rozszerzenie postgis do bazy danych, zeby moc korzystac z typów geometrycznych i funkcji geograficznych
        await conn.run_sync(Base.metadata.create_all)
        await conn.commit()

# async def init_db_and_session(app):
#     engine = create_async_engine(settings.DATABASE_URL, echo=False)
#     async_session = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

#     async with engine.begin() as conn:
#         await conn.run_sync(Base.metadata.create_all)

#     app.state.engine = engine
#     app.state.async_session = async_session
# from fastapi import Request
# from sqlalchemy.ext.asyncio import AsyncSession

# async def get_db(request: Request) -> AsyncSession:
#     async_session = request.app.state.async_session
#     async with async_session() as session:
#         yield session

async def get_db():
    async with AsyncSessionLocal() as session:
        try:
            yield session
        finally:
            await session.close()