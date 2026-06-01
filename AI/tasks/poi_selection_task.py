from __future__ import annotations

import logging
from math import asin, cos, radians, sin, sqrt

from schemas import PoiCandidate, SelectedPoi

logger = logging.getLogger(__name__)


class PoiSelectionTask:
    def __init__(
        self,
        category_ranks: dict[str, int] | None = None,
        redis_client=None,
        session_id: str | None = None,
        seen_key_prefix: str = "poi:seen:",
        seen_ttl: int = 7200,
    ):
        self.redis_client = redis_client
        self.session_id = session_id
        self.seen_key_prefix = seen_key_prefix
        self.seen_ttl = seen_ttl
        self.category_ranks = category_ranks or {
            "museum": 1,
            "castle": 1,
            "fort": 1,
            "ruins": 1,
            "attraction": 1,
            "monument": 2,
            "memorial": 2,
            "artwork": 3,
            "viewpoint": 3,
        }

    def run(
        self,
        candidates: list[PoiCandidate],
        user_latitude: float,
        user_longitude: float,
    ) -> SelectedPoi | None:
        if not candidates:
            return None

        unseen = self._filter_seen(candidates)
        pool = unseen if unseen else candidates

        ranked = sorted(
            pool,
            key=lambda poi: self._selection_score(
                poi=poi,
                user_latitude=user_latitude,
                user_longitude=user_longitude,
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
            "Selected POI: %s, category: %s, distance: %.2fkm, score: %.2f",
            selected.name,
            selected.category,
            distance_km,
            self._selection_score(
                poi=selected,
                user_latitude=user_latitude,
                user_longitude=user_longitude,
            ),
        )
        logger.debug(
            "Top POI candidates: %s",
            [
                {
                    "name": poi.name,
                    "category": poi.category,
                    "distance_km": round(
                        self._haversine_distance(
                            user_latitude,
                            user_longitude,
                            poi.lat,
                            poi.lon,
                        ),
                        3,
                    ),
                    "score": round(
                        self._selection_score(
                            poi=poi,
                            user_latitude=user_latitude,
                            user_longitude=user_longitude,
                        ),
                        2,
                    ),
                    "wikipedia": bool(poi.wikipedia),
                    "wikidata": bool(poi.wikidata),
                }
                for poi in ranked[:5]
            ],
        )

        self._mark_seen(selected.name)

        return SelectedPoi(
            poi=selected,
            distance_km=distance_km,
            category_rank=category_rank,
        )

    def run_many(
        self,
        candidates: list[PoiCandidate],
        user_latitude: float,
        user_longitude: float,
        n: int = 5,
    ) -> list[SelectedPoi]:
        if not candidates:
            return []

        unseen = self._filter_seen(candidates)
        pool = unseen if unseen else candidates

        ranked = sorted(
            pool,
            key=lambda poi: self._selection_score(
                poi=poi,
                user_latitude=user_latitude,
                user_longitude=user_longitude,
            ),
        )

        results = []
        for poi in ranked[:n]:
            distance_km = self._haversine_distance(user_latitude, user_longitude, poi.lat, poi.lon)
            results.append(SelectedPoi(
                poi=poi,
                distance_km=distance_km,
                category_rank=self.category_ranks.get(poi.category, 99),
            ))
            self._mark_seen(poi.name)

        logger.info("Selected %d POIs for planning mode", len(results))
        return results

    def _filter_seen(self, candidates: list) -> list:
        if not self.redis_client or not self.session_id:
            return candidates
        try:
            seen = self.redis_client.smembers(f"{self.seen_key_prefix}{self.session_id}")
            return [c for c in candidates if c.name.strip().lower() not in seen]
        except Exception:
            logger.warning("Redis smembers failed; skipping dedup filter")
            return candidates

    def _mark_seen(self, name: str) -> None:
        if not self.redis_client or not self.session_id:
            return
        try:
            key = f"{self.seen_key_prefix}{self.session_id}"
            self.redis_client.sadd(key, name.strip().lower())
            self.redis_client.expire(key, self.seen_ttl)
        except Exception:
            logger.warning("Redis sadd/expire failed; POI '%s' not marked as seen", name)

    def _selection_score(
        self,
        poi: PoiCandidate,
        user_latitude: float,
        user_longitude: float,
    ) -> float:
        distance_km = self._haversine_distance(
            user_latitude,
            user_longitude,
            poi.lat,
            poi.lon,
        )
        distance_penalty = distance_km * 5
        category_penalty = self.category_ranks.get(poi.category, 99)
        popularity_bonus = self._popularity_bonus(poi)

        return distance_penalty + category_penalty - popularity_bonus

    @staticmethod
    def _popularity_bonus(poi: PoiCandidate) -> float:
        bonus = 0.0
        if poi.wikipedia:
            bonus += 8.0
        if poi.wikidata:
            bonus += 5.0
        if poi.website:
            bonus += 2.0
        if poi.description:
            bonus += 1.0
        return bonus

    @staticmethod
    def _haversine_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        lon1, lat1, lon2, lat2 = map(radians, [lon1, lat1, lon2, lat2])

        dlon = lon2 - lon1
        dlat = lat2 - lat1
        a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
        c = 2 * asin(sqrt(a))

        return 6371 * c
