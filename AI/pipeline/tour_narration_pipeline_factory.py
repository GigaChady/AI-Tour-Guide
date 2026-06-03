from __future__ import annotations

from pipeline.tour_narration_pipeline import TourNarrationPipeline
from schemas import NarrationSettings


class TourNarrationPipelineFactory:
    def __init__(self, user_agent: str = "my-user-agent"):
        self.user_agent = user_agent

    def create(
        self,
        narration_settings: NarrationSettings,
        session_id: str | None = None,
        redis_client=None,
    ) -> TourNarrationPipeline:
        from agents.filtering.cloud_narration_context_agent import (
            CloudNarrationContextAgent,
        )
        from agents.narrative_generation.cloud_narrative_agent import CloudNarrativeAgent
        from integrations.search.wikimedia_search_client import WikimediaSearchClient
        from tasks.information_filtering_task import InformationFilteringTask
        from tasks.location_discovery_task import LocationDiscoveryTask
        from tasks.narration_generation_task import NarrationGenerationTask
        from tasks.poi_enrichment_task import PoiEnrichmentTask
        from connections.configs.redis_config import RedisWorkerConfig
        from tasks.poi_selection_task import PoiSelectionTask
        _cfg = RedisWorkerConfig()

        search_client = WikimediaSearchClient(user_agent=self.user_agent)

        return TourNarrationPipeline(
            narration_settings=narration_settings,
            location_discovery_step=LocationDiscoveryTask(
                narration_settings=narration_settings,
                user_agent=self.user_agent,
            ),
            poi_selection_step=PoiSelectionTask(
                redis_client=redis_client,
                session_id=session_id,
                seen_key_prefix=_cfg.poi_seen_prefix,
                seen_ttl=_cfg.poi_seen_ttl,
            ),
            poi_enrichment_step=PoiEnrichmentTask(search_client=search_client),
            information_filtering_step=InformationFilteringTask(
                filtering_agent=CloudNarrationContextAgent(
                    narration_settings=narration_settings
                )
            ),
            narration_generation_step=NarrationGenerationTask(
                narrative_generation_agent=CloudNarrativeAgent(
                    narration_settings=narration_settings
                )
            ),
        )
