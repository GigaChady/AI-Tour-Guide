import logging

import requests
from geopy.geocoders import Nominatim

from utils.schemas import NarrationSettings, NarrationDetailLevel


class LocationProcessor:
    def __init__(self, narration_settings: NarrationSettings, user_agent: str):
        self.narration_settings = narration_settings
        self.geolocator = Nominatim(user_agent=user_agent)

    def get_address(self, lat: float, lon: float, language_tag: str = "en", zoom_level: NarrationDetailLevel = NarrationDetailLevel.DETAILED):
        location = self.geolocator.reverse(f"{lat}, {lon}", language=language_tag, zoom=zoom_level.value)
        return location.raw if location else {}

    def get_nearby_pois(self, lat: float, lon: float, radius: int = 50):
        query = f"""
        [out:json][timeout:15];
        (
          node["tourism"](around:{radius},{lat},{lon});
          node["historic"](around:{radius},{lat},{lon});
          way["tourism"](around:{radius},{lat},{lon});
          way["historic"](around:{radius},{lat},{lon});
        );
        out center;
        """

        OVERPASS_SERVERS = [
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass.openstreetmap.fr/api/interpreter",
            "https://overpass.nchc.org.tw/api/interpreter"
        ]

        for url in OVERPASS_SERVERS:
            try:
                logging.info(f"Attempting to get nearby POIs from server: {url}")
                response = requests.get(url, params={'data': query}, timeout=15)

                if response.status_code == 200:
                    data = response.json()
                    pois = []
                    for element in data.get('elements', []):
                        # TODO: It is possible to extract more information, for example images, websites, artists, authors; print(f"ELEMENT: {element}")
                        tags = element.get('tags', {})
                        name = tags.get('name')
                        t_type = tags.get('tourism') or tags.get('historic')

                        ignored_types = ["information", "hotel", "guest_house", "picnic_site"]
                        if name and t_type not in ignored_types:
                            pois.append({
                                "name": name,
                                "category": t_type,
                                "lat": element.get('lat') or element.get('center', {}).get('lat'),
                                "lon": element.get('lon') or element.get('center', {}).get('lon')
                            })
                    if len(pois) == 0:
                        logging.warning(f"Could not find any POIs nearby: {self.narration_settings.latitude}, {self.narration_settings.longitude} (search radius: {self.narration_settings.search_radius})")
                    return list({p['name']: p for p in pois}.values())

                elif response.status_code == 429:
                    logging.warning(f"Server {url} refused to respond: HTTP 429 Too Many Requests")
                    continue

            except requests.exceptions.RequestException as e:
                logging.warning(f"Server {url} returned an exception: {e}")
                continue

        logging.error(f"Could no get nearby POIs from any server")
        return []

    def get_location_details(self):
        logging.info("Starting location processing...")
        location_address = self.get_address(
            lat=self.narration_settings.latitude,
            lon=self.narration_settings.longitude,
            language_tag=self.narration_settings.language.language_tag,
            zoom_level=self.narration_settings.detail_level
        )

        points_of_interest = self.get_nearby_pois(
            lat=self.narration_settings.latitude,
            lon=self.narration_settings.longitude,
            radius=self.narration_settings.search_radius
        )

        logging.info("Finished location processing")
        return {
            "location_address": location_address,
            "points_of_interest": points_of_interest
        }