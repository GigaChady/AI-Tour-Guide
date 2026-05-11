from domain.pipeline_models import LocationAddress, LocationDiscoveryResult, PoiCandidate
from pipeline.tour_narration_pipeline import TourNarrationPipeline
from tasks.information_filtering_task import InformationFilteringTask
from tasks.narration_generation_task import NarrationGenerationTask
from tasks.poi_enrichment_task import PoiEnrichmentTask
from tasks.poi_selection_task import PoiSelectionTask
from utils.schemas import NarrationDetailLevel, NarrationLanguage, NarrationSettings


class FakeLocationProcessor:
    def get_location_details(self):
        return LocationDiscoveryResult(
            address=LocationAddress(
                raw={
                    "address": {
                        "city": "Krakow",
                        "suburb": "Old Town",
                    }
                }
            ),
            candidates=[
                PoiCandidate(
                    name="Town Hall Tower",
                    category="monument",
                    lat=50.0616,
                    lon=19.9373,
                    description="Historic tower",
                )
            ],
        )


class FakeScrapingAgent:
    def __init__(self):
        self.query = None

    def run_scraping(self):
        return "Raw historical information"


class FakeFilteringAgent:
    def filter_information(self, poi_name, raw_text):
        return f"Filtered facts about {poi_name}: {raw_text}"


class FakeNarrativeGenerationAgent:
    def generate_narration(self, location_name: str, location_info: str):
        return {
            "location": location_name,
            "narration": f"Narration based on {location_info}",
        }


def _pipeline(include_narration=True, scraping_agent=None):
    return TourNarrationPipeline(
        narration_settings=_settings(include_narration=include_narration),
        location_discovery_step=FakeLocationProcessor(),
        poi_selection_step=PoiSelectionTask(),
        poi_enrichment_step=PoiEnrichmentTask(
            search_agent=scraping_agent or FakeScrapingAgent()
        ),
        information_filtering_step=InformationFilteringTask(
            filtering_agent=FakeFilteringAgent()
        ),
        narration_generation_step=NarrationGenerationTask(
            narrative_generation_agent=FakeNarrativeGenerationAgent()
        ),
    )


def _settings(include_narration=True):
    return NarrationSettings(
        latitude=50.0614,
        longitude=19.9372,
        detail_level=NarrationDetailLevel.DETAILED,
        search_radius=100,
        language=NarrationLanguage(language_name="Polish", language_tag="pl"),
        user_preferences="history",
        include_narration=include_narration,
    )


def test_pipeline_runs_all_steps_and_returns_narration():
    scraping_agent = FakeScrapingAgent()
    pipeline = _pipeline(scraping_agent=scraping_agent)

    result = pipeline.run()

    assert result.poi is not None
    assert result.poi.name == "Town Hall Tower"
    assert result.enriched_poi is not None
    assert result.enriched_poi.raw_information == "Raw historical information"
    assert result.filtered_facts is not None
    assert "Filtered facts" in result.filtered_facts.facts
    assert result.narration is not None
    assert result.narration.location == "Town Hall Tower"
    assert scraping_agent.query == (
        "Information and history about Town Hall Tower Krakow Old Town"
    )


def test_pipeline_skips_narration_when_disabled():
    pipeline = _pipeline(include_narration=False)

    result = pipeline.run()

    assert result.poi is not None
    assert result.enriched_poi is not None
    assert result.filtered_facts is None
    assert result.narration is None
