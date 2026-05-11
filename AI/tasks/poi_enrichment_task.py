from __future__ import annotations

import logging

from domain.pipeline_models import EnrichedPoi, LocationAddress, SelectedPoi

logger = logging.getLogger(__name__)


class PoiEnrichmentTask:
    def __init__(self, search_agent):
        self.search_agent = search_agent

    def run(
        self,
        selected_poi: SelectedPoi,
        address: LocationAddress,
    ) -> EnrichedPoi:
        query = self._build_query(selected_poi=selected_poi, address=address)
        self.search_agent.query = query

        raw_information = self.search_agent.run_scraping() or ""
        logger.info(
            "Raw location information for POI '%s': %s",
            selected_poi.poi.name,
            raw_information,
        )

        return EnrichedPoi(
            poi=selected_poi.poi,
            address=address,
            raw_information=raw_information,
        )

    @staticmethod
    def _build_query(selected_poi: SelectedPoi, address: LocationAddress) -> str:
        return (
            f"Information and history about {selected_poi.poi.name} "
            f"{address.city} {address.suburb}"
        )
