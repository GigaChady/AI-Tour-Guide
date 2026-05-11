from domain.pipeline_models import LocationAddress, PoiCandidate, SelectedPoi
from tasks.information_filtering_task import InformationFilteringTask
from tasks.narration_generation_task import NarrationGenerationTask
from tasks.poi_enrichment_task import PoiEnrichmentTask
from utils.schemas import NarrationDetailLevel, NarrationLanguage, NarrationSettings


class FakeSearchAgent:
    def __init__(self):
        self.query = None

    def run_scraping(self):
        return "Search result"


class FakeFilteringAgent:
    def filter_information(self, poi_name, raw_text):
        return f"Facts for {poi_name}: {raw_text}"


class FakeNarrationAgent:
    def generate_narration(self, location_name, location_info):
        return {
            "location": location_name,
            "narration": f"Narration from {location_info}",
        }


def _settings():
    return NarrationSettings(
        latitude=50.0614,
        longitude=19.9372,
        detail_level=NarrationDetailLevel.DETAILED,
        search_radius=100,
        language=NarrationLanguage(language_name="Polish", language_tag="pl"),
        user_preferences="history",
    )


def test_poi_enrichment_task_builds_query_and_returns_enriched_model():
    search_agent = FakeSearchAgent()
    task = PoiEnrichmentTask(search_agent=search_agent)
    selected = SelectedPoi(
        poi=PoiCandidate(
            name="Town Hall Tower",
            category="monument",
            lat=50.0616,
            lon=19.9373,
        ),
        distance_km=0.1,
        category_rank=2,
    )
    address = LocationAddress(
        raw={"address": {"city": "Krakow", "suburb": "Old Town"}}
    )

    enriched = task.run(selected_poi=selected, address=address)

    assert search_agent.query == (
        "Information and history about Town Hall Tower Krakow Old Town"
    )
    assert enriched.poi.name == "Town Hall Tower"
    assert enriched.raw_information == "Search result"


def test_filtering_and_narration_tasks_return_domain_models():
    selected = SelectedPoi(
        poi=PoiCandidate(
            name="Town Hall Tower",
            category="monument",
            lat=50.0616,
            lon=19.9373,
        ),
        distance_km=0.1,
        category_rank=2,
    )
    enriched = PoiEnrichmentTask(search_agent=FakeSearchAgent()).run(
        selected_poi=selected,
        address=LocationAddress(),
    )

    facts = InformationFilteringTask(
        filtering_agent=FakeFilteringAgent()
    ).run(enriched)
    narration = NarrationGenerationTask(
        narrative_generation_agent=FakeNarrationAgent()
    ).run(facts, _settings())

    assert facts.facts == "Facts for Town Hall Tower: Search result"
    assert narration.location == "Town Hall Tower"
    assert "Facts for Town Hall Tower" in narration.narration
