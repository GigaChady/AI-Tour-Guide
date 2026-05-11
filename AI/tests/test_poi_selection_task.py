from domain.pipeline_models import PoiCandidate
from tasks.poi_selection_task import PoiSelectionTask


def test_poi_selection_prefers_nearest_candidate():
    task = PoiSelectionTask()
    candidates = [
        PoiCandidate(name="Far Museum", category="museum", lat=50.10, lon=19.90),
        PoiCandidate(name="Near Artwork", category="artwork", lat=50.001, lon=19.001),
    ]

    selected = task.run(
        candidates=candidates,
        user_latitude=50.0,
        user_longitude=19.0,
    )

    assert selected is not None
    assert selected.poi.name == "Near Artwork"


def test_poi_selection_uses_category_rank_when_distance_matches():
    task = PoiSelectionTask()
    candidates = [
        PoiCandidate(name="Artwork", category="artwork", lat=50.0, lon=19.0),
        PoiCandidate(name="Museum", category="museum", lat=50.0, lon=19.0),
    ]

    selected = task.run(
        candidates=candidates,
        user_latitude=50.0,
        user_longitude=19.0,
    )

    assert selected is not None
    assert selected.poi.name == "Museum"
