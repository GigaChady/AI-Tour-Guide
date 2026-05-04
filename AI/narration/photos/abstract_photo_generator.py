from abc import ABC, abstractmethod


class AbstractPhotoGenerator(ABC):
    def __init__(self):
        pass

    @abstractmethod
    def generate(self, image_type: str):
        pass