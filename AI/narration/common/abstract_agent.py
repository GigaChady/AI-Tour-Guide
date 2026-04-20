from abc import ABC

from narration.common.narration_settings import NarrationSettings


class AbstractAgent(ABC):
    def __init__(self, narration_settings: NarrationSettings):
        self.narration_settings = narration_settings