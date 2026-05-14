from __future__ import annotations

import logging
from math import asin, cos, radians, sin, sqrt

from schemas import PoiCandidate, SelectedPoi

logger = logging.getLogger(__name__)


class PoiSelectionTask:
    def __init__(self, category_ranks: dict[str, int] | None = None):
        self.category_ranks = category_ranks or {
            "museum": 1,
            "castle": 1,
            "fort": 1,
            "ruins": 1,
            "monument": 2,
            "memorial": 2,
            "artwork": 3,
            "viewpoint": 3,
            "attraction": 3,
        }

    def run(
        self,
        candidates: list[PoiCandidate],
        user_latitude: float,
        user_longitude: float,
    ) -> SelectedPoi | None:
        if not candidates:
            return None

        ranked = sorted(
            candidates,
            key=lambda poi: (
                self._haversine_distance(
                    user_latitude,
                    user_longitude,
                    poi.lat,
                    poi.lon,
                ),
                self.category_ranks.get(poi.category, 99),
            ),
        )

        selected = ranked[0]
        distance_km = self._haversine_distance(
            user_latitude,
            user_longitude,
            selected.lat,
            selected.lon,
        )
        category_rank = self.category_ranks.get(selected.category, 99)

        logger.info(
            "Selected POI: %s, category: %s, distance: %.2fkm",
            selected.name,
            selected.category,
            distance_km,
        )

        return SelectedPoi(
            poi=selected,
            distance_km=distance_km,
            category_rank=category_rank,
        )

    @staticmethod
    def _haversine_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        lon1, lat1, lon2, lat2 = map(radians, [lon1, lat1, lon2, lat2])

        dlon = lon2 - lon1
        dlat = lat2 - lat1
        a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
        c = 2 * asin(sqrt(a))

        return 6371 * c
