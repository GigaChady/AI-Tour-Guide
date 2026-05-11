from __future__ import annotations

import logging

from domain.pipeline_models import LocationAddress, LocationDiscoveryResult, PoiCandidate
from integrations.geocoding.nominatim_client import NominatimClient
from integrations.overpass.overpass_client import OverpassClient
from integrations.overpass.overpass_poi_parser import OverpassPoiParser
from utils.schemas import NarrationDetailLevel, NarrationSettings

logger = logging.getLogger(__name__)


class LocationProcessor:
    def __init__(
        self,
        narration_settings: NarrationSettings,
        user_agent: str,
        geocoding_client: NominatimClient | None = None,
        overpass_client: OverpassClient | None = None,
        poi_parser: OverpassPoiParser | None = None,
    ):
        self.narration_settings = narration_settings
        self.geocoding_client = geocoding_client or NominatimClient(
            user_agent=user_agent,
        )
        self.overpass_client = overpass_client or OverpassClient(
            user_agent=user_agent,
        )
        self.poi_parser = poi_parser or OverpassPoiParser()

    def get_address(
        self,
        lat: float,
        lon: float,
        language_tag: str = "en",
        zoom_level: NarrationDetailLevel = NarrationDetailLevel.DETAILED,
    ) -> LocationAddress:
        return self.geocoding_client.reverse_geocode(
            lat=lat,
            lon=lon,
            language_tag=language_tag,
            zoom_level=zoom_level,
        )

    def get_nearby_pois(
        self,
        lat: float,
        lon: float,
        radius: int = 50,
    ) -> list[PoiCandidate]:
        data = self.overpass_client.get_nearby_poi_data(
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

    def get_location_details(self) -> LocationDiscoveryResult:
        logger.info("Starting location processing...")

        location_address = self.get_address(
            lat=self.narration_settings.latitude,
            lon=self.narration_settings.longitude,
            language_tag=self.narration_settings.language.language_tag,
            zoom_level=self.narration_settings.detail_level,
        )

        points_of_interest = self.get_nearby_pois(
            lat=self.narration_settings.latitude,
            lon=self.narration_settings.longitude,
            radius=self.narration_settings.search_radius,
        )

        logger.info("Finished location processing")

        return LocationDiscoveryResult(
            address=location_address,
            candidates=points_of_interest,
        )
