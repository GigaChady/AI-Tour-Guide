from abc import ABC

from utils.schemas import NarrationSettings


class AbstractAgent(ABC):
    def __init__(self, narration_settings: NarrationSettings):
        self.narration_settings = narration_settings