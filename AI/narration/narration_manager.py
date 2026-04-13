from langchain_community.tools import DuckDuckGoSearchRun

from AI.narration.filtering.filtering_agent import FilteringAgent
from AI.narration.location.location_processor import LocationProcessor
from AI.narration.common.narration_settings import NarrationSettings
from AI.narration.narrative_generation.narrative_generation_agent import OllamaNarrativeGenerationAgent
from AI.narration.scraping.scraping_agent import LangChainScrapingAgent


class NarrationManager:
    def __init__(self, narration_settings: NarrationSettings, location_processor: LocationProcessor, scraping_agent: LangChainScrapingAgent, filtering_agent: FilteringAgent, narrative_generation_agent: OllamaNarrativeGenerationAgent):
        self.narration_settings = narration_settings
        self.location_processor = location_processor
        self.scraping_agent = scraping_agent
        self.filtering_agent = filtering_agent
        self.narrative_generation_agent = narrative_generation_agent

    def get_narration(self):

        location_details = self.location_processor.get_location_details()
        poi = self.scraping_agent.select_best_poi(location_details["points_of_interest"])
        self.scraping_agent.query = LangChainScrapingAgent.build_query(poi, location_details["location_address"])

        location_raw_information = self.scraping_agent.run_scraping()

        location_filtered_information = self.filtering_agent.filter_information(poi["name"], location_raw_information)

        narration = self.narrative_generation_agent.generate_narration(location_name=poi["name"], location_info=location_filtered_information)
        return narration

    @staticmethod
    def build_narration_manager(narration_settings: NarrationSettings):
        narration_manager = NarrationManager(
            narration_settings=narration_settings,
            location_processor=LocationProcessor(narration_settings=narration_settings, user_agent="my-user-agent"),
            scraping_agent=LangChainScrapingAgent(narration_settings=narration_settings, search_tool=DuckDuckGoSearchRun()),
            filtering_agent=FilteringAgent(narration_settings=narration_settings),
            narrative_generation_agent=OllamaNarrativeGenerationAgent(narration_settings=narration_settings)
        )
        return narration_manager
