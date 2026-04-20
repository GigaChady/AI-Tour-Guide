import os
from enum import Enum

class NarrationLanguage:
    def __init__(self, language_name: str, language_tag: str):
        self.language_name = language_name
        self.language_tag = language_tag

class NarrationDetailLevel(Enum):
    DETAILED = 18
    STREET = 16
    DISTRICT = 14
    CITY = 10
    COUNTRY = 3

class NarrationSettings:
    def __init__(self, latitude: float, longitude: float, detail_level: NarrationDetailLevel, search_radius: int, language: NarrationLanguage, user_preferences: str, ollama_base_url: str = None):
        """
        Constructs a narration settings object.
        :param latitude: latitude of the location
        :param longitude: longitude of the location
        :param detail_level: narration detail level
        :param search_radius: POI search radius around user's position
        :param language: narration language
        :param user_preferences: user's interests, main narration aspects
        :param ollama_base_url: Ollama base URL (default from OLLAMA_BASE_URL env or http://localhost:11434)
        """
        self.latitude = latitude
        self.longitude = longitude
        self.detail_level = detail_level
        self.search_radius = search_radius
        self.language = language
        self.user_preferences = user_preferences
        self.ollama_base_url = ollama_base_url or os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
