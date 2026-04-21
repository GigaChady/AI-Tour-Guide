from __future__ import annotations

import os
from enum import Enum
from typing import Any

from pydantic import BaseModel, ConfigDict, Field

# ====== BACKEND CONNECTIONS =====

class LocationEvent(BaseModel):
    # Strict contract for messages pushed by Backend to Redis stream `location:events`.
    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "examples": [
                {
                    "session_id": "8b7d1c6e-6f9f-4ee9-b75a-3eb7d795c2f8",
                    "lat": "52.2297",
                    "lng": "21.0122",
                }
            ]
        },
    )

    session_id: str = Field(min_length=1, description="Tour session identifier")
    lat: float = Field(description="Latitude, accepts numeric strings")
    lng: float = Field(description="Longitude, accepts numeric strings")

    def __str__(self):
        return f"lat {self.lat} lng {self.lng}"


class PreferencesEvent(BaseModel):
    # Loose contract for now

    model_config = ConfigDict(
        extra="allow"
    )

    data: Any = None

    def __str__(self):
        return f"{self.data}"

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

    # @classmethod
    # def from_text(
    #     cls,
    #     text: str,
    #     *,
    #     language: str | None = None,
    #     title: str | None = None,
    #     poi_ids: list[str] | None = None,
    #     metadata: dict[str, Any] | None = None,
    # ) -> "NarrationMessage":
    #     return cls(
    #         data=NarrationData(
    #             text=text,
    #             language=language,
    #             title=title,
    #             poi_ids=poi_ids or [],
    #             metadata=metadata or {},
    #         )
    #     )

class Poi(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: str
    name: str = ""
    lat: float
    lng: float
    description: str | None = None
    image_url: str | None = None
    image_base64: str | None = None


class NarrationData(BaseModel): # Deprecated
    model_config = ConfigDict(extra="ignore")

    text: str = ""
    language: str | None = None
    title: str | None = None
    poi_ids: list[str] = Field(default_factory=list)
    metadata: dict[str, Any] = Field(default_factory=dict)


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
        default_factory=lambda: os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
    )








