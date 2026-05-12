from __future__ import annotations

import logging
from schemas import LocationAddress
from schemas import NarrationDetailLevel

logger = logging.getLogger(__name__)


class NominatimClient:
    def __init__(self, user_agent: str):
        from geopy.geocoders import Nominatim

        self.geolocator = Nominatim(user_agent=user_agent)

    def reverse_geocode(
        self,
        lat: float,
        lon: float,
        language_tag: str = "en",
        zoom_level: NarrationDetailLevel = NarrationDetailLevel.DETAILED,
    ) -> LocationAddress:
        try:
            location = self.geolocator.reverse(
                f"{lat}, {lon}",
                language=language_tag,
                zoom=zoom_level.value,
                timeout=10,
            )
            return LocationAddress(raw=location.raw if location else {})
        except Exception as e:
            logger.warning("Could not reverse geocode location: %s", e)
            return LocationAddress()
