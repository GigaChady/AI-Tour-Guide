from __future__ import annotations

from pipeline.steps import (
    InformationFilteringStep,
    LocationDiscoveryStep,
    NarrationGenerationStep,
    PoiEnrichmentStep,
    PoiSelectionStep,
)
from tasks.information_filtering_task import InformationFilteringTask
from tasks.narration_generation_task import NarrationGenerationTask
from tasks.poi_enrichment_task import PoiEnrichmentTask
from tasks.poi_selection_task import PoiSelectionTask
from domain.pipeline_models import TourPipelineResult
from utils.schemas import NarrationSettings


class TourNarrationPipeline:
    def __init__(
        self,
        narration_settings: NarrationSettings,
        location_discovery_step: LocationDiscoveryStep,
        poi_selection_step: PoiSelectionStep,
        poi_enrichment_step: PoiEnrichmentStep,
        information_filtering_step: InformationFilteringStep,
        narration_generation_step: NarrationGenerationStep,
    ):
        self.narration_settings = narration_settings
        self.location_discovery_step = location_discovery_step
        self.poi_selection_step = poi_selection_step
        self.poi_enrichment_step = poi_enrichment_step
        self.information_filtering_step = information_filtering_step
        self.narration_generation_step = narration_generation_step

    def run(self) -> TourPipelineResult:
        discovery = self.location_discovery_step.get_location_details()

        selected = self.poi_selection_step.run(
            candidates=discovery.candidates,
            user_latitude=self.narration_settings.latitude,
            user_longitude=self.narration_settings.longitude,
        )
        if selected is None:
            return TourPipelineResult()

        enriched = self.poi_enrichment_step.run(
            selected_poi=selected,
            address=discovery.address,
        )

        if not self.narration_settings.include_narration:
            return TourPipelineResult(
                selected_poi=selected,
                enriched_poi=enriched,
            )

        filtered = self.information_filtering_step.run(enriched)
        narration = self.narration_generation_step.run(
            filtered_facts=filtered,
            narration_settings=self.narration_settings,
        )

        return TourPipelineResult(
            selected_poi=selected,
            enriched_poi=enriched,
            filtered_facts=filtered,
            narration=narration,
        )

    @staticmethod
    def build_default(narration_settings: NarrationSettings) -> "TourNarrationPipeline":
        from langchain_community.tools import DuckDuckGoSearchRun

        from narration.filtering.cloud_filtering_agent import CloudFilteringAgent
        from narration.location.location_processor import LocationProcessor
        from narration.narrative_generation.cloud_narrative_agent import CloudNarrativeAgent
        from narration.scraping.scraping_agent import LangChainScrapingAgent

        search_agent = LangChainScrapingAgent(
            narration_settings=narration_settings,
            search_tool=DuckDuckGoSearchRun(),
        )

        return TourNarrationPipeline(
            narration_settings=narration_settings,
            location_discovery_step=LocationProcessor(
                narration_settings=narration_settings,
                user_agent="my-user-agent",
            ),
            poi_selection_step=PoiSelectionTask(),
            poi_enrichment_step=PoiEnrichmentTask(search_agent=search_agent),
            information_filtering_step=InformationFilteringTask(
                filtering_agent=CloudFilteringAgent(narration_settings=narration_settings)
            ),
            narration_generation_step=NarrationGenerationTask(
                narrative_generation_agent=CloudNarrativeAgent(
                    narration_settings=narration_settings
                )
            ),
        )
