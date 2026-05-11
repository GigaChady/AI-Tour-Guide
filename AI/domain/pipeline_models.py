from __future__ import annotations

from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class PoiCandidate(BaseModel):
    model_config = ConfigDict(extra="ignore")

    name: str
    category: str
    lat: float
    lon: float
    website: str | None = None
    wikidata: str | None = None
    wikipedia: str | None = None
    description: str | None = None

class LocationAddress(BaseModel):
    model_config = ConfigDict(extra="ignore")

    raw: dict[str, Any] = Field(default_factory=dict)

    @property
    def city(self) -> str:
        address = self.raw.get("address", {})
        return address.get("city", "")

    @property
    def suburb(self) -> str:
        address = self.raw.get("address", {})
        return address.get("suburb", "")


class LocationDiscoveryResult(BaseModel):
    model_config = ConfigDict(extra="ignore")

    address: LocationAddress = Field(default_factory=LocationAddress)
    candidates: list[PoiCandidate] = Field(default_factory=list)


class SelectedPoi(BaseModel):
    model_config = ConfigDict(extra="ignore")

    poi: PoiCandidate
    distance_km: float
    category_rank: int


class EnrichedPoi(BaseModel):
    model_config = ConfigDict(extra="ignore")

    poi: PoiCandidate
    address: LocationAddress = Field(default_factory=LocationAddress)
    raw_information: str


class FilteredPoiFacts(BaseModel):
    model_config = ConfigDict(extra="ignore")

    poi: PoiCandidate
    facts: str


class NarrationResult(BaseModel):
    model_config = ConfigDict(extra="ignore")

    location: str
    narration: str

    @classmethod
    def from_model_response(cls, value: dict[str, Any] | str) -> "NarrationResult":
        if isinstance(value, dict):
            return cls(
                location=str(value.get("location", "")),
                narration=str(value.get("narration", "")),
            )

        return cls(location="", narration=value)


class TourPipelineResult(BaseModel):
    model_config = ConfigDict(extra="ignore")

    selected_poi: SelectedPoi | None = None
    enriched_poi: EnrichedPoi | None = None
    filtered_facts: FilteredPoiFacts | None = None
    narration: NarrationResult | None = None

    @property
    def poi(self) -> PoiCandidate | None:
        if self.selected_poi is None:
            return None
        return self.selected_poi.poi
