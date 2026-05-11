import argparse
import asyncio

from app.core.database import init_db, reset_database

from app.core.config import DEFAULT_ONBOARDING_CATALOG
from app.services.token_service import TokenService

token_service = TokenService()

_GENDER_CATALOG = next(i for i in DEFAULT_ONBOARDING_CATALOG if i["question_key"] == "gender")
_INTERESTS_CATALOG = next(i for i in DEFAULT_ONBOARDING_CATALOG if i["question_key"] == "interests")

GENDER_OPTIONS = [a["answer_key"] for a in _GENDER_CATALOG["answers"]]
INTEREST_KEYS = [a["answer_key"] for a in _INTERESTS_CATALOG["answers"]]



async def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--reset-all", action="store_true")

    args = parser.parse_args()
    await init_db()
    if args.reset_all:
        await reset_database()


if __name__ == "__main__":
    asyncio.run(main())
