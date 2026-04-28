import os
from abc import abstractmethod, ABC


class AbstractProcessor(ABC):
    def __init__(self):
        self.is_mock = bool(os.getenv("AI_MOCK", True))

    @abstractmethod
    def process(self, event, preferences):
        pass

    @abstractmethod
    def validate(self, entry_id, payload):
        pass

    @abstractmethod
    def validate_prefs(self, prefs):
        pass