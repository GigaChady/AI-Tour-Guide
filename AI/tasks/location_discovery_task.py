from __future__ import annotations

import logging

from schemas import LocationAddress, LocationDiscoveryResult, PoiCandidate
from integrations.contracts import GeocodingClient, PoiDataClient, PoiParser
from tasks.address_resolution_task import AddressResolutionTask
from tasks.nearby_poi_discovery_task import NearbyPoiDiscoveryTask
from schemas import NarrationDetailLevel, NarrationSettings

logger = logging.getLogger(__name__)


class LocationDiscoveryTask:
    def __init__(
        self,
        narration_settings: NarrationSettings,
        user_agent: str,
        geocoding_client: GeocodingClient | None = None,
        overpass_client: PoiDataClient | None = None,
        poi_parser: PoiParser | None = None,
        address_resolution_task: AddressResolutionTask | None = None,
        nearby_poi_discovery_task: NearbyPoiDiscoveryTask | None = None,
    ):
        self.narration_settings = narration_settings
        self.address_resolution_task = address_resolution_task or AddressResolutionTask(
            user_agent=user_agent,
            geocoding_client=geocoding_client,
        )
        self.nearby_poi_discovery_task = (
            nearby_poi_discovery_task
            or NearbyPoiDiscoveryTask(
                user_agent=user_agent,
                poi_data_client=overpass_client,
                poi_parser=poi_parser,
            )
        )

    def get_address(
        self,
        lat: float,
        lon: float,
        language_tag: str = "en",
        zoom_level: NarrationDetailLevel = NarrationDetailLevel.DETAILED,
    ) -> LocationAddress:
        return self.address_resolution_task.run(
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
        return self.nearby_poi_discovery_task.run(
            lat=lat,
            lon=lon,
            radius=radius,
        )

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
