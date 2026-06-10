import asyncio
from sqlalchemy import select, delete
from app.core.database import AsyncSessionLocal
from app.models.models import User, Route, RoutePoi

# ==========================================
# CLEANER CONFIGURATION
# ==========================================

TARGET_EMAIL = "user3@gmail.com"

# AVAILABLE MODES:
# "planned"   - deletes ONLY routes without a recorded path
# "completed" - deletes ONLY routes with a recorded path
# "all"       - deletes ALL routes for the specified user
MODE = "all"


# ==========================================
# EXECUTION LOGIC
# ==========================================

async def clean_routes():
    async with AsyncSessionLocal() as db:
        # 1. Fetch the target user
        result = await db.execute(select(User).where(User.email == TARGET_EMAIL))
        user = result.scalar_one_or_none()

        if not user:
            print(f"[ERROR] User with email '{TARGET_EMAIL}' not found.")
            return

        print(f"[INFO] Found target user: {user.email}")

        # 2. Build the query based on the selected mode
        query = select(Route.id).where(Route.user_id == user.id)

        if MODE == "planned":
            query = query.where(Route.path == None)
        elif MODE == "completed":
            query = query.where(Route.path != None)
        elif MODE == "all":
            pass  # No additional filters needed
        else:
            print(f"[ERROR] Invalid MODE: '{MODE}'. Please use 'planned', 'completed', or 'all'.")
            return

        # 3. Retrieve IDs of the routes to be deleted
        result = await db.execute(query)
        route_ids = result.scalars().all()

        if not route_ids:
            print(f"[INFO] No '{MODE}' routes found for this user. Nothing to delete.")
            return

        print(f"[INFO] Found {len(route_ids)} route(s) matching mode '{MODE}'. Starting deletion...")

        # 4. First, delete associated POIs to prevent Foreign Key constraint errors
        await db.execute(delete(RoutePoi).where(RoutePoi.route_id.in_(route_ids)))

        # 5. Then, delete the actual routes
        await db.execute(delete(Route).where(Route.id.in_(route_ids)))

        # 6. Commit the transaction
        await db.commit()

        print(f"[SUCCESS] Successfully deleted {len(route_ids)} route(s) and their associated POIs!")


if __name__ == "__main__":
    asyncio.run(clean_routes())

# Command to run inside docker:
# docker exec -it ai_tour_guide-api-1 python -m dev_scripts.clean_route_history