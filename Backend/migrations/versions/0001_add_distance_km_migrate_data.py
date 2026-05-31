"""add distance_km column and migrate data from distance_m

Revision ID: 0001
Revises:
Create Date: 2026-05-31

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa

revision: str = "0001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Dodaj nową kolumnę distance_km (nullable, bo dane zostaną uzupełnione w kroku 2)
    op.add_column("routes", sa.Column("distance_km", sa.Float(), nullable=True))

    # 2. Przepisz dane: przelicz distance_m → distance_km dla istniejących wierszy
    op.execute(
        """
        UPDATE routes
        SET distance_km = ROUND((distance_m / 1000.0)::numeric, 2)
        WHERE distance_m IS NOT NULL
        """
    )

    # 3. Ustaw NOT NULL teraz gdy dane są uzupełnione, z domyślną wartością 0 dla wierszy bez distance_m
    op.execute("UPDATE routes SET distance_km = 0 WHERE distance_km IS NULL")
    op.alter_column("routes", "distance_km", nullable=False)


def downgrade() -> None:
    op.drop_column("routes", "distance_km")
