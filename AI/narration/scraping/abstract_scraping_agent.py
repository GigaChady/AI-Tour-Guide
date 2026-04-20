from abc import abstractmethod

from narration.common.abstract_agent import AbstractAgent
from narration.common.narration_settings import NarrationSettings


class AbstractScrapingAgent(AbstractAgent):
    def __init__(self, narration_settings: NarrationSettings):
        super().__init__(narration_settings)

    @abstractmethod
    def run_scraping(self):
        pass