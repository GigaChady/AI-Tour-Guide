from schemas import PoiCandidate, SelectedPoi, TourPipelineResult
from processors.backend_processor import BackendProcessor
from processors.narration_processor import NarrationProcessor
from schemas import (
    LocationEvent,
    NarrationDetailLevel,
    NarrationLanguage,
    NarrationMessage,
    NarrationSettings,
    Poi,
    PoisMessage,
    PreferencesEvent,
)


class FakeNarrationProcessor:
    def __init__(self):
        self.validated = None
        self.processed = None

    def validate(self, event, preferences):
        self.validated = (event, preferences)
        return NarrationSettings(
            latitude=event.lat,
            longitude=event.lng,
            detail_level=NarrationDetailLevel.DETAILED,
            search_radius=100,
            language=NarrationLanguage(language_name="Polish", language_tag="pl"),
            user_preferences="history",
        )

    def process(self, session_id, narration_settings):
        self.processed = (session_id, narration_settings)
        return (
            NarrationMessage(text="Narration"),
            PoisMessage(
                data=[
                    Poi(
                        name="Museum",
                        lat=narration_settings.latitude,
                        lng=narration_settings.longitude,
                    )
                ]
            ),
        )


class FakePhotoProcessor:
    def __init__(self):
        self.calls = []

    def generate(self, category, count):
        self.calls.append((category, count))
        return f"https://example.test/{category}-{count}.jpg"


class FakePipeline:
    def __init__(self, result):
        self.result = result
        self.was_run = False

    def run(self):
        self.was_run = True
        return self.result


class FakePipelineFactory:
    def __init__(self, pipeline):
        self.pipeline = pipeline
        self.settings = None

    def create(self, narration_settings):
        self.settings = narration_settings
        return self.pipeline


def test_backend_processor_uses_named_narration_processor_dependency():
    narration_processor = FakeNarrationProcessor()
    processor = BackendProcessor(
        narration_processor=narration_processor,
        is_mock=False,
    )
    event = LocationEvent(session_id="session-1", lat=50.0, lng=19.0)
    preferences = PreferencesEvent(interests=["history"])

    session_id, narration_json, pois_json = processor.process(event, preferences)

    assert session_id == "session-1"
    assert narration_processor.validated == (event, preferences)
    assert narration_processor.processed[0] == "session-1"
    assert '"type":"narration"' in narration_json
    assert '"type":"pois"' in pois_json


def test_narration_processor_uses_named_photo_processor_dependency():
    photo_processor = FakePhotoProcessor()
    processor = NarrationProcessor(photo_processor=photo_processor)
    poi = PoiCandidate(
        name="Museum",
        category="museum",
        lat=50.0,
        lon=19.0,
    )

    photos = processor._generate_photo_urls(poi, photo_count=2)

    assert photos == [
        "https://example.test/museum-0.jpg",
        "https://example.test/museum-1.jpg",
    ]
    assert photo_processor.calls == [("museum", 0), ("museum", 1)]


def test_narration_processor_uses_named_pipeline_factory_dependency():
    poi = PoiCandidate(
        name="Museum",
        category="museum",
        lat=50.0,
        lon=19.0,
    )
    settings = NarrationSettings(
        latitude=50.0,
        longitude=19.0,
        detail_level=NarrationDetailLevel.DETAILED,
        search_radius=100,
        language=NarrationLanguage(language_name="Polish", language_tag="pl"),
        user_preferences="history",
        photo_count=0,
    )
    pipeline = FakePipeline(
        TourPipelineResult(
            selected_poi=SelectedPoi(
                poi=poi,
                distance_km=0.0,
                category_rank=1,
            )
        )
    )
    pipeline_factory = FakePipelineFactory(pipeline)
    processor = NarrationProcessor(
        photo_processor=None,
        pipeline_factory=pipeline_factory,
    )

    narration, pois = processor.process("session-1", settings)

    assert pipeline_factory.settings == settings
    assert pipeline.was_run is True
    assert narration is None
    assert pois.data[0].name == "Museum"
