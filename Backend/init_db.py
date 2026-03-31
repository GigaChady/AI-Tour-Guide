import asyncio
from app.core.config import settings
from app.core.database import init_db


if __name__ == "__main__":
    asyncio.run(init_db())
