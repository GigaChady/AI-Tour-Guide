from abc import abstractmethod, ABC


class AbstractProcessor(ABC):
    def __init__(self):
        pass

    @abstractmethod
    def process(self, client, entry_id, payload, pubsub_prefix) -> None:
        pass