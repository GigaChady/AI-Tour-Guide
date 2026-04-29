from storage.minio_image_storage import MinioImageStorage
from utils.schemas import Poi, NarrationMessage, PoisMessage, PreferencesEvent


def _mock_pois(session_id: str, lat: float, lng: float) -> list[Poi]:
    base_id = session_id.replace(" ", "-") or "session"
    pois=[
        Poi(
            name="Mock Museum",
            lat=lat + 0.0005,
            lng=lng + 0.0005,
            desc="Mock point of interest generated from Redis Stream input.",
            photos = [_mock_image_url("Mock Museum", "utils/assets/mock_images/museum_mock.jpg")]

        ),
        Poi(
            name="Mock Viewpoint",
            lat=lat - 0.0004,
            lng=lng + 0.0003,
            desc="Second mock POI for testing backend integration.",
            photos=[_mock_image_url("Mock Viewpoint", "utils/assets/mock_images/viewpoint_mock.jpg")]

        )
        ]
    return pois


def _mock_narration(session_id: str, preferences: PreferencesEvent, lat: float, lng: float, pois: list[Poi], narration=None) -> NarrationMessage:
    text = (
        f"Mock narration for session {session_id}. "
        f"User is near {lat:.5f}, {lng:.5f}. "
        f"Suggested stops: {', '.join(p.name for p in pois)}."
        f"User preferences: {preferences}."
        f"Real narration: {narration}"
    )
    return NarrationMessage(text=text)


def _mock_image_url(poi_name: str, filepath: str) -> str:
    # In a real implementation, this would fetch or generate an image URL based on the POI name
    with open(filepath, "rb") as f:
        image_bytes = f.read()
    storage = MinioImageStorage()

    storage_name = poi_name.replace(" ", "_").lower() + ".jpg"

    result = storage.upload_bytes(storage_name, image_bytes) # WARNING: swithc to poi_id in production to avoid duplicates
    return result["image_url"]
