import asyncio
from sqlalchemy import select


from app.core.database import AsyncSessionLocal
from app.models.models import User

# ==========================================
# KONFIGURACJA
# ==========================================
TARGET_EMAIL = "user9@gmail.com"  # Wpisz tutaj email konta, na które się logujesz

async def make_admin():
    async with AsyncSessionLocal() as db:
        result = await db.execute(select(User).where(User.email == TARGET_EMAIL))
        user = result.scalar_one_or_none()

        if not user:
            print(f"[ERROR] Nie znaleziono użytkownika o emailu '{TARGET_EMAIL}'.")
            return

        if user.is_admin:
            print(f"[INFO] Użytkownik '{TARGET_EMAIL}' ma już uprawnienia administratora.")
            return

        # Ustawiamy flagę administratora na True
        user.is_admin = True
        await db.commit()

        print(f"[SUCCESS] Konto '{TARGET_EMAIL}' otrzymało uprawnienia administratora! Odśwież aplikację.")

if __name__ == "__main__":
    asyncio.run(make_admin())

# docker exec -it ai_tour_guide-api-1 python -m dev_scripts.make_admin