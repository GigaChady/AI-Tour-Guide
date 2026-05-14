from __future__ import annotations

from typing import Any, Protocol

from schemas import EnrichedPoi


class InformationFilteringAgent(Protocol):
    def filter_information(self, enriched_poi: EnrichedPoi) -> str:
        ...


class NarrativeGenerationAgent(Protocol):
    def generate_narration(
        self,
        location_name: str,
        location_info: str,
    ) -> dict[str, Any] | str:
        ...
