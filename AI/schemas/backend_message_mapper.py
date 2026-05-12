from __future__ import annotations

from typing import Any

from schemas.backend import (
    LocationEvent,
    NarrationMessage,
    Poi,
    PoisMessage,
    PreferencesEvent,
)
from schemas.narration import (
    NarrationDetailLevel,
    NarrationLanguage,
    NarrationSettings,
)
from schemas.pipeline import NarrationResult, PoiCandidate


class BackendMessageMapper:
    def build_narration_settings(
        self,
        location: LocationEvent,
        prefs: PreferencesEvent,
        defaults: Any | None = None,
    ) -> NarrationSettings:
        if not isinstance(location, LocationEvent) or not isinstance(prefs, PreferencesEvent):
            raise ValueError("Expected LocationEvent and PreferencesEvent instances")

        defaults = defaults or self._load_default_settings()

        detail_level_value = getattr(
            prefs,
            "detail_level",
            defaults.default_detail_level.value,
        )
        search_radius = getattr(
            prefs,
            "search_radius",
            defaults.default_search_radius,
        )
        language_tag = getattr(
            prefs,
            "language",
            defaults.default_language,
        )
        language_name = getattr(
            prefs,
            "language_name",
            defaults.default_language_name,
        )
        user_preferences = getattr(
            prefs,
            "user_preferences",
            ", ".join(prefs.interests) if prefs.interests else "",
        )

        raw_is_narration = getattr(location, "is_narration", None)
        include_narration = True if raw_is_narration is None else bool(raw_is_narration)

        raw_photo_count = getattr(location, "include_photos", None)
        photo_count = 2 if raw_photo_count is None else int(raw_photo_count)
        photo_count = max(1, photo_count)

        return NarrationSettings(
            latitude=location.lat,
            longitude=location.lng,
            detail_level=NarrationDetailLevel(detail_level_value),
            search_radius=search_radius,
            language=NarrationLanguage(
                language_name=language_name,
                language_tag=language_tag,
            ),
            user_preferences=user_preferences,
            ollama_base_url=defaults.ollama_base_url,
            include_narration=include_narration,
            photo_count=photo_count,
        )

    def build_narration_message(self, narration: NarrationResult) -> NarrationMessage:
        return NarrationMessage(text=narration.narration)

    def build_pois_message(
        self,
        poi: PoiCandidate,
        photos: list[str],
    ) -> PoisMessage:
        poi_response = Poi(
            name=poi.name,
            photos=photos,
            desc=poi.name,
            lat=poi.lat,
            lng=poi.lon,
        )

        return PoisMessage(data=[poi_response])

    @staticmethod
    def _load_default_settings():
        from configs.narration_configs import (
            NarrationDefaultsConfig,
        )

        return NarrationDefaultsConfig()
