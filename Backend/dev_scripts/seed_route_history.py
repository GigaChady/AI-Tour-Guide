import asyncio
import random
import string
from datetime import datetime, timedelta, timezone
from sqlalchemy import select
from geoalchemy2.elements import WKTElement

from app.core.database import AsyncSessionLocal
from app.models.models import User, Route, RoutePoi

# ==========================================
# SEEDER CONFIGURATION
# ==========================================

TARGET_EMAIL = "user@user.pl"

# Toggle this flag to generate a completely random route
GENERATE_RANDOM_ROUTE = True

# --- 1. MANUAL CONFIGURATION (Used if GENERATE_RANDOM_ROUTE is False) ---
MANUAL_ROUTE_CONFIG = {
    "name": "Wrocław City Walk",
    "country": "Poland",
    "city": "Wrocław",
    "distance_m": 4200.0,
    "duration_minutes": 135,
    "pois": [
        {
            "name": "Pasaż Grunwaldzki",
            "lat": 51.1110,
            "lng": 17.0600,
            "description": "Started our journey at the busy city center."
        },
        {
            "name": "Politechnika Wrocławska",
            "lat": 51.1095,
            "lng": 17.0625,
            "description": "The main campus architecture was impressive."
        },
        {
            "name": "Most Zwierzyniecki",
            "lat": 51.1075,
            "lng": 17.0615,
            "description": "Crossed the historic bridge."
        },
        {
            "name": "Hala Stulecia",
            "lat": 51.1065,
            "lng": 17.0775,
            "description": "Finished the walk at the UNESCO heritage site."
        }
    ]
}

# --- 2. RANDOM CONFIGURATION (Used if GENERATE_RANDOM_ROUTE is True) ---
RANDOM_ROUTE_CONFIG = {
    "base_lat": 51.10,
    "base_lng": 17.06,
    "poi_count": 10
}


# ==========================================
# EXECUTION LOGIC
# ==========================================

def get_random_string(length=6):
    return ''.join(random.choices(string.ascii_letters + string.digits, k=length))


async def seed_history():
    async with AsyncSessionLocal() as db:
        result = await db.execute(select(User).where(User.email == TARGET_EMAIL))
        user = result.scalar_one_or_none()

        if not user:
            print(f"[ERROR] User with email '{TARGET_EMAIL}' not found. Create an account first.")
            return

        print(f"[INFO] Found target user: {user.email}")

        start_time = datetime.now(timezone.utc).replace(tzinfo=None) - timedelta(days=1)

        if GENERATE_RANDOM_ROUTE:
            print("[INFO] Mode: Random Data Generation")
            route_name = f"Random Route {get_random_string(4)}"
            country = "Randomland"
            city = "Random City"
            distance_m = random.uniform(2000.0, 15000.0)
            duration_minutes = random.randint(45, 240)

            pois_data = []
            current_lat = RANDOM_ROUTE_CONFIG["base_lat"]
            current_lng = RANDOM_ROUTE_CONFIG["base_lng"]

            for i in range(RANDOM_ROUTE_CONFIG["poi_count"]):
                pois_data.append({
                    "name": f"Random POI {get_random_string(3)}",
                    "lat": current_lat,
                    "lng": current_lng,
                    "description": f"Automatically generated description {get_random_string(10)}"
                })
                current_lat += random.uniform(-0.005, 0.005)
                current_lng += random.uniform(-0.005, 0.005)
        else:
            print("[INFO] Mode: Manual Configuration")
            route_name = MANUAL_ROUTE_CONFIG["name"]
            country = MANUAL_ROUTE_CONFIG["country"]
            city = MANUAL_ROUTE_CONFIG["city"]
            distance_m = MANUAL_ROUTE_CONFIG["distance_m"]
            duration_minutes = MANUAL_ROUTE_CONFIG["duration_minutes"]
            pois_data = MANUAL_ROUTE_CONFIG["pois"]

        end_time = start_time + timedelta(minutes=duration_minutes)

        if len(pois_data) < 2:
            print("[ERROR] A route must have at least 2 POIs to generate a valid path line.")
            return

        # Generate WKT LINESTRING (Format: "LINESTRING(lng1 lat1, lng2 lat2, ...)")
        linestring_points = ", ".join([f"{p['lng']} {p['lat']}" for p in pois_data])
        linestring = f"LINESTRING({linestring_points})"

        route = Route(
            user_id=user.id,
            name=route_name,
            country=country,
            city=city,
            started_at=start_time,
            ended_at=end_time,
            distance_m=distance_m,
            path=WKTElement(linestring, srid=4326)
        )

        db.add(route)
        await db.flush()

        route_pois = [
            RoutePoi(
                route_id=route.id,
                name=p["name"],
                lat=p["lat"],
                lng=p["lng"],
                description=p["description"]
            )
            for p in pois_data
        ]

        db.add_all(route_pois)
        await db.commit()

        print(f"[SUCCESS] Route '{route_name}' containing {len(pois_data)} POIs has been successfully seeded!")


if __name__ == "__main__":
    asyncio.run(seed_history())

# Command to run inside docker:
# docker exec -it ai_tour_guide-api-1 python -m dev_scripts.seed_route_history
