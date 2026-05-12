from __future__ import annotations

from typing import Protocol

from schemas import LocationAddress, PoiCandidate
from integrations.overpass.overpass_models import OverpassResponse
from schemas import NarrationDetailLevel


class GeocodingClient(Protocol):
    def reverse_geocode(
        self,
        lat: float,
        lon: float,
        language_tag: str = "en",
        zoom_level: NarrationDetailLevel = NarrationDetailLevel.DETAILED,
    ) -> LocationAddress:
        ...


class PoiDataClient(Protocol):
    def get_nearby_poi_data(
        self,
        lat: float,
        lon: float,
        radius: int = 50,
    ) -> OverpassResponse | None:
        ...


class PoiParser(Protocol):
    def parse(self, data: OverpassResponse) -> list[PoiCandidate]:
        ...


class SearchClient(Protocol):
    def search(self, query: str) -> str:
        ...
