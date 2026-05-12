from __future__ import annotations

import logging

from schemas import PoiCandidate
from integrations.contracts import PoiDataClient, PoiParser
from integrations.overpass.overpass_client import OverpassClient
from integrations.overpass.overpass_poi_parser import OverpassPoiParser

logger = logging.getLogger(__name__)


class NearbyPoiDiscoveryTask:
    def __init__(
        self,
        user_agent: str,
        poi_data_client: PoiDataClient | None = None,
        poi_parser: PoiParser | None = None,
    ):
        self.poi_data_client = poi_data_client or OverpassClient(
            user_agent=user_agent,
        )
        self.poi_parser = poi_parser or OverpassPoiParser()

    def run(
        self,
        lat: float,
        lon: float,
        radius: int = 50,
    ) -> list[PoiCandidate]:
        data = self.poi_data_client.get_nearby_poi_data(
            lat=lat,
            lon=lon,
            radius=radius,
        )
        if data is None:
            return []

        pois = self.poi_parser.parse(data)

        if not pois:
            logger.warning(
                "No POIs found nearby lat=%s lon=%s radius=%s",
                lat,
                lon,
                radius,
            )

        return pois
