from __future__ import annotations

import os
from enum import Enum
from typing import Any

from pydantic import BaseModel, ConfigDict, Field

# ====== BACKEND CONNECTIONS =====

class LocationEvent(BaseModel):
    # Strict contract for messages pushed by Backend to Redis stream `location:events`.
    model_config = ConfigDict(
        extra="ignore",
    )

    session_id: str = Field(min_length=1, description="Tour session identifier")
    lat: float = Field(description="Latitude, accepts numeric strings")
    lng: float = Field(description="Longitude, accepts numeric strings")
    include_photos: int | None = None
    is_narration: bool | None = None

    def __str__(self):
        return f"lat {self.lat} lng {self.lng}"


class PreferencesEvent(BaseModel):
    # Loose contract for now

    model_config = ConfigDict(
        extra="allow"
    )

    interests: list[str] | None = None

    def __str__(self):
        return f"{self.interests.__str__()}"

class PoisMessage(BaseModel):
    """
    PoisMessage represents a message containing a list of Points of Interest (POIs) related to a specific location event.
    """
    model_config = ConfigDict(extra="ignore")

    type: str = "pois"
    data: list[Poi] = Field(default_factory=list)


class NarrationMessage(BaseModel):
    """
    NarrationMessage represents a message containing a list of Narrations related to a specific location event.
    """
    model_config = ConfigDict(extra="ignore")

    type: str = "narration"
    text: str

class Poi(BaseModel):
    model_config = ConfigDict(extra="ignore")

    name: str
    photos: list[str] = Field(default_factory=list) # As a list of paths to photos
    desc: str | None = None
    lat: float
    lng: float



# ====== INTERNAL MODELS =====


class NarrationLanguage(BaseModel):
    model_config = ConfigDict(extra="ignore")

    language_name: str
    language_tag: str


class NarrationDetailLevel(Enum):
    DETAILED = 18
    STREET = 16
    DISTRICT = 14
    CITY = 10
    COUNTRY = 3


class NarrationSettings(BaseModel):
    model_config = ConfigDict(extra="ignore")

    latitude: float
    longitude: float
    detail_level: NarrationDetailLevel
    search_radius: int
    language: NarrationLanguage
    user_preferences: str
    ollama_base_url: str = Field(
        default_factory=lambda: os.getenv("OLLAMA_BASE_URL", "http://ollama:11434")
    )









