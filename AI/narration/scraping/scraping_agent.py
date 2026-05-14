import logging
from math import radians, cos, sin, asin, sqrt

from langchain_core.tools import BaseTool, ToolException
from typing_extensions import override

from utils.schemas import NarrationSettings
from narration.scraping.abstract_scraping_agent import AbstractScrapingAgent


class LangChainScrapingAgent(AbstractScrapingAgent):
    def __init__(self, narration_settings: NarrationSettings, search_tool: BaseTool):
        super().__init__(narration_settings)
        self.search_tool = search_tool

        self.query = None
        self.category_ranks = {
            "museum": 1, "castle": 1, "fort": 1, "ruins": 1,
            "monument": 2, "memorial": 2,
            "artwork": 3, "viewpoint": 3, "attraction": 3
        }

    def _haversine_distance(self, lat1, lon1, lat2, lon2):
        """
        Calculate the great circle distance between two points
        on the earth (specified in decimal degrees).
        Returns distance in kilometers.
        """
        # Convert decimal degrees to radians
        lon1, lat1, lon2, lat2 = map(radians, [lon1, lat1, lon2, lat2])

        # Haversine formula
        dlon = lon2 - lon1
        dlat = lat2 - lat1
        a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
        c = 2 * asin(sqrt(a))
        km = 6371 * c  # Radius of earth in kilometers
        return km

    def select_best_poi(self, pois):
        if not pois:
            return None

        # Sort by distance first (lower is better), then by category (lower is better)
        sorted_pois = sorted(
            pois,
            key=lambda x: (
                self._haversine_distance(
                    self.narration_settings.latitude,
                    self.narration_settings.longitude,
                    x.get('lat', 0),
                    x.get('lon', 0)
                ),
                self.category_ranks.get(x['category'], 99)
            )
        )
        logging.info(f"Selected POI: {sorted_pois[0]['name']}, category: {sorted_pois[0]['category']}, distance: {self._haversine_distance(self.narration_settings.latitude, self.narration_settings.longitude, sorted_pois[0].get('lat', 0), sorted_pois[0].get('lon', 0)):.2f}km")
        return sorted_pois[0]

    @override
    def run_scraping(self):
        logging.info(f"Starting query execution \"{self.query}\"")

        try:
            result = self.search_tool.run(self.query)
        except ToolException as e:
            return logging.error(f"Error executing query \"{self.query}\": {e}")

        logging.info(f"Finished query execution \"{self.query}\"")
        return result

    @staticmethod
    def build_query(poi, address_data):
        # print(f"poi: {poi}")
        # print(f"address_data: {address_data}")
        poi_name = poi['name']
        city = address_data["address"].get("city", "")

        suburb = address_data["address"]["suburb"]
        return f"Information and history about {poi_name} {city} {suburb}"