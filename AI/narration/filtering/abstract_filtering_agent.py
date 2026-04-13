from AI.narration.common.abstract_agent import AbstractAgent
from AI.narration.common.narration_settings import NarrationSettings


class AbstractFilteringAgent(AbstractAgent):
    def __init__(self, narration_settings: NarrationSettings):
        super().__init__(narration_settings)