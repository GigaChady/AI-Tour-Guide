from abc import abstractmethod

from AI.narration.common.abstract_agent import AbstractAgent
from AI.narration.common.narration_settings import NarrationSettings


class AbstractNarrativeGenerationAgent(AbstractAgent):
    def __init__(self, narration_settings: NarrationSettings):
        super().__init__(narration_settings)

    @abstractmethod
    def generate_narration(self, location_name: str, location_info: str):
        pass