from os import path

from narration.photos.abstract_photo_generator import AbstractPhotoGenerator


class DefaultPhotoGenerator(AbstractPhotoGenerator):
    def __init__(self, storage, photo_path="utils/assets/default_photos"):
        super().__init__()
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
            "fort": "building",
            "ruins": "building",
            "monument": "building",
            "memorial": "building",
            "artwork": "building",
            # Natural/scenic
            "viewpoint": "landscape",
            # General attractions
            "attraction": "district",
        }
        self.storage = storage

    def generate(self, image_type: str):
        if not image_type in self.poi_category_to_image_type:
            image_type = "default"

        image = self.mapping.get(image_type)

        filepath = path.join(self.photo_path, image)

        with open(filepath, "rb") as f:
            image_bytes = f.read()

        storage_name = image

        result = self.storage.upload_bytes(storage_name, image_bytes)  # WARNING: swithc to poi_id in production to avoid duplicates
        return result["image_url"]

