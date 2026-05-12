from __future__ import annotations

from schemas import LocationAddress
from integrations.contracts import GeocodingClient
from integrations.geocoding.nominatim_client import NominatimClient
from schemas import NarrationDetailLevel


class AddressResolutionTask:
    def __init__(
        self,
        user_agent: str,
        geocoding_client: GeocodingClient | None = None,
    ):
        self.geocoding_client = geocoding_client or NominatimClient(
            user_agent=user_agent,
        )

    def run(
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
