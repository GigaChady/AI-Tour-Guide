from __future__ import annotations

from typing import Protocol

from schemas import (
    LocationEvent,
    NarrationMessage,
    NarrationSettings,
    PoisMessage,
    PreferencesEvent,
)


class PhotoGenerator(Protocol):
    def generate(self, image_type: str, count: int) -> str | None:
        ...


class StreamProcessor(Protocol):
    def validate(self, entry_id, payload, **kwargs):
        ...

    def validate_prefs(self, prefs):
        ...

    def process(self, event, preferences):
        ...


class NarrationService(Protocol):
    def validate(
        self,
        location: LocationEvent,
        prefs: PreferencesEvent,
        **kwargs,
    ) -> NarrationSettings:
        ...

    def process(
        self,
        session_id: str,
        narration_settings: NarrationSettings,
        **kwargs,
    ) -> tuple[NarrationMessage | None, PoisMessage | None]:
        ...
