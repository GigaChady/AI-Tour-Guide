from utils.schemas import Poi, NarrationMessage, PoisMessage, PreferencesEvent


def _mock_pois(session_id: str, lat: float, lng: float) -> list[Poi]:
    base_id = session_id.replace(" ", "-") or "session"
    pois=[
        Poi(
            id=f"{base_id}-poi-1",
            name="Mock Museum",
            lat=lat + 0.0005,
            lng=lng + 0.0005,
            description="Mock point of interest generated from Redis Stream input.",
            image_url=None,
            image_base64=None,
        ),
        Poi(
            id=f"{base_id}-poi-2",
            name="Mock Viewpoint",
            lat=lat - 0.0004,
            lng=lng + 0.0003,
            description="Second mock POI for testing backend integration.",
            image_url=None,
            image_base64=None,
        )
        ]
    return pois


def _mock_narration(session_id: str, preferences: PreferencesEvent, lat: float, lng: float, pois: list[Poi]) -> NarrationMessage:
    text = (
        f"Mock narration for session {session_id}. "
        f"User is near {lat:.5f}, {lng:.5f}. "
        f"Suggested stops: {', '.join(p.name for p in pois)}."
        f"User preferences: {preferences}."
    )
    return NarrationMessage(text=text)
