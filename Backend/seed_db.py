import argparse
import asyncio
import random
import secrets
from datetime import datetime, timedelta, timezone

from geoalchemy2.elements import WKTElement
from sqlalchemy import delete

from app.core.config import DEFAULT_ONBOARDING_CATALOG, settings
from app.core.database import AsyncSessionLocal, init_db, reset_database
from app.models.models import RefreshToken, Route, RoutePoi, User, UserPreferences
from app.services.token_service import TokenService

token_service = TokenService()

FIRST_NAMES = ["Anna", "Piotr", "Kasia", "Marek", "Ola", "Tomasz", "Julia", "Kacper", "Maja", "Jakub"]
LAST_NAMES = ["Nowak", "Kowalski", "Wiśniewski", "Wójcik", "Kowalczyk", "Kamiński", "Lewandowski", "Zieliński", "Szymański", "Dąbrowski"]
CITIES = ["Wrocław", "Kraków", "Warszawa", "Gdańsk", "Poznań", "Łódź", "Katowice", "Szczecin"]

_GENDER_CATALOG = next(i for i in DEFAULT_ONBOARDING_CATALOG if i["question_key"] == "gender")
_INTERESTS_CATALOG = next(i for i in DEFAULT_ONBOARDING_CATALOG if i["question_key"] == "interests")

GENDER_OPTIONS = [a["answer_key"] for a in _GENDER_CATALOG["answers"]]
INTEREST_KEYS = [a["answer_key"] for a in _INTERESTS_CATALOG["answers"]]


def _random_wkt_line() -> WKTElement:
    start_lon = round(random.uniform(16.8, 17.3), 6)
    start_lat = round(random.uniform(51.0, 51.2), 6)
    points = []
    for _ in range(random.randint(3, 6)):
        start_lon += random.uniform(0.001, 0.01)
        start_lat += random.uniform(0.001, 0.01)
        points.append(f"{start_lon:.6f} {start_lat:.6f}")
    return WKTElement(f"LINESTRING({', '.join(points)})", srid=4326)


async def _clear_existing_data() -> None:
    async with AsyncSessionLocal() as session:
        for model in [RefreshToken, RoutePoi, Route, UserPreferences, User]:
            await session.execute(delete(model))
        await session.commit()


async def _seed_users(count: int) -> None:
    hashed_password = token_service.hash_password(settings.SEED_USER_PASSWORD)

    async with AsyncSessionLocal() as session:
        for index in range(count):
            first_name = random.choice(FIRST_NAMES)
            last_name = random.choice(LAST_NAMES)
            email = f"{first_name.lower()}.{last_name.lower()}.{secrets.token_hex(4)}@example.com"

            user = User(
                email=email,
                hashed_password=hashed_password,
                name=first_name,
                gender=random.choice(GENDER_OPTIONS),
            )
            session.add(user)
            await session.flush()

            session.add(UserPreferences(
                user_id=user.id,
                interests=random.sample(INTEREST_KEYS, k=random.randint(1, len(INTEREST_KEYS))),
            ))

            for _ in range(random.randint(0, 2)):
                expires_at = datetime.now(timezone.utc) + timedelta(days=random.randint(7, 30))
                session.add(RefreshToken(
                    user_id=user.id,
                    token_hash=secrets.token_hex(32),
                    expires_at=expires_at,
                    revoked=random.choice([False, False, False, True]),
                ))

            for _ in range(random.randint(0, 3)):
                session.add(Route(
                    user_id=user.id,
                    city=random.choice(CITIES),
                    name=f"{random.choice(CITIES)} walk {index + 1}",
                    path=_random_wkt_line(),
                    distance_m=round(random.uniform(1200, 18000), 2),
                    started_at=datetime.now(timezone.utc).replace(tzinfo=None) - timedelta(days=random.randint(0, 30)),
                    ended_at=None,
                ))

        await session.commit()


async def main() -> None:
    parser = argparse.ArgumentParser(description="Seed the database with sample data.")
    parser.add_argument("--reset-all", action="store_true", help="Drop and recreate all tables before seeding.")
    parser.add_argument("--reset", action="store_true", help="Delete existing rows before seeding.")
    parser.add_argument("--users", type=int, default=12, help="Number of users to create.")
    args = parser.parse_args()

    await init_db()

    if args.reset_all:
        await reset_database()
    elif args.reset:
        await _clear_existing_data()

    await _seed_users(args.users)

    print(f"Seed completed. Created {args.users} users (password: {settings.SEED_USER_PASSWORD}).")


if __name__ == "__main__":
    asyncio.run(main())
