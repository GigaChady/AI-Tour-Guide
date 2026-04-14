import argparse
import asyncio
import random
import secrets
from datetime import datetime, timedelta, timezone

from geoalchemy2.elements import WKTElement
from sqlalchemy import delete, select

from app.core.config import DEFAULT_ONBOARDING_CATALOG, settings
from app.core.database import AsyncSessionLocal, init_db, reset_database
from app.models.models import (
    DemographicsGenderOption,
    RefreshToken,
    Route,
    User,
    UserPreferences,
)
from app.services.token_service import TokenService

token_service = TokenService()

FIRST_NAMES = ["Anna", "Piotr", "Kasia", "Marek", "Ola", "Tomasz", "Julia", "Kacper", "Maja", "Jakub"]
LAST_NAMES = ["Nowak", "Kowalski", "Wiśniewski", "Wójcik", "Kowalczyk", "Kamiński", "Lewandowski", "Zieliński", "Szymański", "Dąbrowski"]
CITIES = ["Wrocław", "Kraków", "Warszawa", "Gdańsk", "Poznań", "Łódź", "Katowice", "Szczecin"]

_GENDER_CATALOG = next(i for i in DEFAULT_ONBOARDING_CATALOG if i["question_key"] == "gender")
_INTERESTS_CATALOG = next(i for i in DEFAULT_ONBOARDING_CATALOG if i["question_key"] == "interests")

GENDER_OPTIONS = [(a["answer_key"], a["title"]) for a in _GENDER_CATALOG["answers"]]
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
        for model in [
            RefreshToken,
            Route,
            UserPreferences,
            User,
            DemographicsGenderOption,
        ]:
            await session.execute(delete(model))
        await session.commit()


async def _seed_reference_data() -> dict[str, int]:
    gender_id_by_code: dict[str, int] = {}

    async with AsyncSessionLocal() as session:
        existing_genders = {
            row.code: row
            for row in (await session.execute(select(DemographicsGenderOption))).scalars().all()
        }
        for sort_order, (code, label) in enumerate(GENDER_OPTIONS, start=1):
            option = existing_genders.get(code)
            if option is None:
                option = DemographicsGenderOption(code=code, label=label, is_active=True, sort_order=sort_order)
                session.add(option)
                await session.flush()
            gender_id_by_code[code] = option.id

        await session.commit()

    return gender_id_by_code


async def _seed_users(count: int, gender_id_by_code: dict[str, int]) -> None:
    hashed_password = token_service.hash_password(settings.SEED_USER_PASSWORD)

    async with AsyncSessionLocal() as session:
        for index in range(count):
            first_name = random.choice(FIRST_NAMES)
            last_name = random.choice(LAST_NAMES)
            email = f"{first_name.lower()}.{last_name.lower()}.{secrets.token_hex(4)}@example.com"
            gender_code = random.choice(list(gender_id_by_code.keys()))

            user = User(
                email=email,
                hashed_password=hashed_password,
                is_active=True,
                imie=first_name,
                name=first_name,
                nazwisko=last_name,
                gender_option_id=gender_id_by_code[gender_code],
                gender_custom=None,
                wiek=round(random.uniform(18, 70), 1),
            )
            session.add(user)
            await session.flush()

            session.add(
                UserPreferences(
                    user_id=user.id,
                    interests=[{"answer_keys": random.sample(INTEREST_KEYS, k=random.randint(1, len(INTEREST_KEYS)))}],
                )
            )

            for _ in range(random.randint(0, 2)):
                expires_at = datetime.now(timezone.utc) + timedelta(days=random.randint(7, 30))
                session.add(
                    RefreshToken(
                        user_id=user.id,
                        token_hash=secrets.token_hex(32),
                        expires_at=expires_at,
                        revoked=random.choice([False, False, False, True]),
                    )
                )

            for _ in range(random.randint(0, 3)):
                session.add(
                    Route(
                        user_id=user.id,
                        city=random.choice(CITIES),
                        name=f"{random.choice(CITIES)} walk {index + 1}",
                        path=_random_wkt_line(),
                        distance_m=round(random.uniform(1200, 18000), 2),
                        started_at=datetime.now(timezone.utc).replace(tzinfo=None) - timedelta(days=random.randint(0, 30)),
                        ended_at=None,
                    )
                )

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

    gender_id_by_code = await _seed_reference_data()
    await _seed_users(args.users, gender_id_by_code)

    print(f"Seed completed. Created {args.users} users (password: {settings.SEED_USER_PASSWORD}) plus reference data.")


if __name__ == "__main__":
    asyncio.run(main())
