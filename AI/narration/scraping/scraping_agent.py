import logging

from langchain_core.tools import BaseTool, ToolException
from typing_extensions import override

from AI.narration.common.narration_settings import NarrationSettings
from AI.narration.scraping.abstract_scraping_agent import AbstractScrapingAgent


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

    def select_best_poi(self, pois):
        if not pois:
            return None

        sorted_pois = sorted(
            pois,
            key=lambda x: self.category_ranks.get(x['category'], 99)
        )
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