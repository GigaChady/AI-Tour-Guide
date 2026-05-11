from __future__ import annotations

from domain.pipeline_models import EnrichedPoi, FilteredPoiFacts


class InformationFilteringTask:
    def __init__(self, filtering_agent):
        self.filtering_agent = filtering_agent

    def run(self, enriched_poi: EnrichedPoi) -> FilteredPoiFacts:
        return FilteredPoiFacts(
            poi=enriched_poi.poi,
            facts=self.filtering_agent.filter_information(
                enriched_poi.poi.name,
                enriched_poi.raw_information,
            ),
        )
