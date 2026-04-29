import os
from abc import abstractmethod, ABC


class AbstractProcessor(ABC):
    def __init__(self, sub_processor = None):
        self.is_mock = os.getenv("AI_MOCK", False).lower() in ("true", "1", "t")
        self.sub_processor = sub_processor

    @abstractmethod
    def process(self, event, preferences):
        pass

    @abstractmethod
    def validate(self, entry_id, payload, **kwargs):
        pass

    @abstractmethod
    def validate_prefs(self, prefs):
        pass