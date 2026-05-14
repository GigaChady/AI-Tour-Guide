from __future__ import annotations

import logging

from schemas import (
    EnrichedPoi,
    LocationAddress,
    PoiInformationSource,
    SelectedPoi,
)
from integrations.contracts import SearchClient

logger = logging.getLogger(__name__)


def _quoted(value: str) -> str:
    value = value.strip()
    return f'"{value}"' if value else ""


def _general_location(address: LocationAddress) -> str:
    return address.general_location.replace(",", "")


class PoiEnrichmentTask:
    def __init__(self, search_client: SearchClient):
        self.search_client = search_client

    def run(
        self,
        selected_poi: SelectedPoi,
        address: LocationAddress,
    ) -> EnrichedPoi:
        query = self._build_query(selected_poi=selected_poi, address=address)
        search_content = self.search_client.search(query)
        logger.info(
            "Raw location information for POI '%s' query='%s': %s",
            selected_poi.poi.name,
            query,
            search_content[:500],
        )

        return EnrichedPoi(
            poi=selected_poi.poi,
            address=address,
            information_sources=[
                PoiInformationSource(
                    query=query,
                    content=search_content,
                )
            ],
        )

    @staticmethod
    def _build_query(selected_poi: SelectedPoi, address: LocationAddress) -> str:
        query_parts = [
            _quoted(selected_poi.poi.name),
            _quoted(_general_location(address)),
        ]
        return " ".join(part for part in query_parts if part)
