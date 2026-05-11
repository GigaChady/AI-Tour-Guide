from __future__ import annotations

from typing import Protocol

from domain.pipeline_models import (
    EnrichedPoi,
    FilteredPoiFacts,
    LocationAddress,
    LocationDiscoveryResult,
    NarrationResult,
    SelectedPoi,
)
from utils.schemas import NarrationSettings


class LocationDiscoveryStep(Protocol):
    def get_location_details(self) -> LocationDiscoveryResult:
        ...


class PoiSelectionStep(Protocol):
    def run(
        self,
        candidates: list,
        user_latitude: float,
        user_longitude: float,
    ) -> SelectedPoi | None:
        ...


class PoiEnrichmentStep(Protocol):
    def run(
        self,
        selected_poi: SelectedPoi,
        address: LocationAddress,
    ) -> EnrichedPoi:
        ...


class InformationFilteringStep(Protocol):
    def run(self, enriched_poi: EnrichedPoi) -> FilteredPoiFacts:
        ...


class NarrationGenerationStep(Protocol):
    def run(
        self,
        filtered_facts: FilteredPoiFacts,
        narration_settings: NarrationSettings,
    ) -> NarrationResult:
        ...
