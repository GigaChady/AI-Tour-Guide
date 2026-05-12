import logging
from os import path

from storage.contracts import ImageStorage

logger = logging.getLogger(__name__)

class PhotoProcessor:
    """
    Class for handling photo generation and processing.
    """
    def __init__(self, storage: ImageStorage, photo_path="utils/assets/default_photos"):
        self.photo_path = photo_path
        self.mapping = {
            "building": "def_building.jpg",
            "district": "def_district.jpg",
            "landscape": "def_landscape.jpg",
            "default": "def_default.jpg",
        }
        # Map OpenStreetMap categories to image types
        self.poi_category_to_image_type = {
            # Buildings and structures
            "museum": "building",
            "castle": "building",
            "fort": "landscape",
            "ruins": "landscape",
            "monument": "building",
            "memorial": "district",
            "artwork": "default",
            # Natural/scenic
            "viewpoint": "landscape",
            # General attractions
            "attraction": "district",
        }
        self.storage = storage

    def generate(self, image_type: str, count: int):
        if not image_type in self.poi_category_to_image_type or count > 0:
            image_type = "default"
        else:
            image_type = self.poi_category_to_image_type[image_type]

        image = self.mapping.get(image_type)

        logger.info("Selected image %s for category %s", image, image_type)

        filepath = path.join(self.photo_path, image)

        with open(filepath, "rb") as f:
            image_bytes = f.read()

        storage_name = image

        result = self.storage.upload_bytes(storage_name, image_bytes)  # WARNING: swithc to poi_id in production to avoid duplicates
        return result["image_url"]

