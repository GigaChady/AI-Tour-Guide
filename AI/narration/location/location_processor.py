import logging
import time
import random
from typing import Any

import requests
from geopy.geocoders import Nominatim
from requests import Session

from utils.schemas import NarrationSettings, NarrationDetailLevel

logger = logging.getLogger(__name__)


class LocationProcessor:
    OVERPASS_SERVERS = [
        "https://overpass.openstreetmap.fr/api/interpreter",
        "https://overpass-api.de/api/interpreter",
        "https://overpass.nchc.org.tw/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
    ]

    RETRYABLE_STATUS_CODES = {408, 429, 500, 502, 503, 504}

    def __init__(self, narration_settings: NarrationSettings, user_agent: str):
        self.narration_settings = narration_settings
        self.geolocator = Nominatim(user_agent=user_agent)
        self.session = Session()
        self.session.headers.update({
            "User-Agent": user_agent
        })

    def get_address(
        self,
        lat: float,
        lon: float,
        language_tag: str = "en",
        zoom_level: NarrationDetailLevel = NarrationDetailLevel.DETAILED,
    ) -> dict[str, Any]:
        try:
            location = self.geolocator.reverse(
                f"{lat}, {lon}",
                language=language_tag,
                zoom=zoom_level.value,
                timeout=10,
            )
            return location.raw if location else {}
        except Exception as e:
            logger.warning("Could not reverse geocode location: %s", e)
            return {}

    def _build_overpass_query(self, lat: float, lon: float, radius: int) -> str:
        return f"""
        [out:json][timeout:10];
        (
          node["tourism"](around:{radius},{lat},{lon});
          node["historic"](around:{radius},{lat},{lon});
          way["tourism"](around:{radius},{lat},{lon});
          way["historic"](around:{radius},{lat},{lon});
        );
        out center tags;
        """

    def _request_overpass(
        self,
        url: str,
        query: str,
        max_retries: int = 2,
        timeout: int = 12,
    ) -> dict[str, Any] | None:
        for attempt in range(max_retries + 1):
            try:
                logger.info(
                    "Requesting Overpass server=%s attempt=%s/%s",
                    url,
                    attempt + 1,
                    max_retries + 1,
                )

                response = self.session.post(
                    url,
                    data={"data": query},
                    timeout=timeout,
                )

                logger.info(
                    "Overpass response server=%s status=%s",
                    url,
                    response.status_code,
                )

                if response.status_code == 200:
                    return response.json()

                if response.status_code not in self.RETRYABLE_STATUS_CODES:
                    logger.warning(
                        "Non-retryable Overpass response from %s: HTTP %s, body=%s",
                        url,
                        response.status_code,
                        response.text[:300],
                    )
                    return None

                retry_after = response.headers.get("Retry-After")
                if retry_after and retry_after.isdigit():
                    sleep_seconds = int(retry_after)
                else:
                    sleep_seconds = min(2 ** attempt, 8) + random.uniform(0, 0.5)

                logger.warning(
                    "Retryable Overpass response from %s: HTTP %s. Sleeping %.2fs",
                    url,
                    response.status_code,
                    sleep_seconds,
                )
                time.sleep(sleep_seconds)

            except requests.exceptions.Timeout:
                sleep_seconds = min(2 ** attempt, 8) + random.uniform(0, 0.5)
                logger.warning(
                    "Overpass timeout from %s. Sleeping %.2fs",
                    url,
                    sleep_seconds,
                )
                time.sleep(sleep_seconds)

            except requests.exceptions.RequestException as e:
                sleep_seconds = min(2 ** attempt, 8) + random.uniform(0, 0.5)
                logger.warning(
                    "Overpass request error from %s: %s. Sleeping %.2fs",
                    url,
                    e,
                    sleep_seconds,
                )
                time.sleep(sleep_seconds)

            except ValueError as e:
                logger.warning("Invalid JSON from Overpass server %s: %s", url, e)
                return None

        return None

    def _parse_pois(self, data: dict[str, Any]) -> list[dict[str, Any]]:
        ignored_types = {"information", "hotel", "guest_house", "picnic_site"}

        pois = []

        for element in data.get("elements", []):
            tags = element.get("tags", {})
            name = tags.get("name")
            poi_type = tags.get("tourism") or tags.get("historic")

            if not name or poi_type in ignored_types:
                continue

            lat = element.get("lat") or element.get("center", {}).get("lat")
            lon = element.get("lon") or element.get("center", {}).get("lon")

            if lat is None or lon is None:
                continue

            pois.append({
                "name": name,
                "category": poi_type,
                "lat": lat,
                "lon": lon,
                "website": tags.get("website"),
                "wikidata": tags.get("wikidata"),
                "wikipedia": tags.get("wikipedia"),
                "description": tags.get("description"),
            })

        # Deduplication by normalized name + category
        deduplicated = {}
        for poi in pois:
            key = (poi["name"].strip().lower(), poi["category"])
            deduplicated[key] = poi

        return list(deduplicated.values())

    def get_nearby_pois(self, lat: float, lon: float, radius: int = 50) -> list[dict[str, Any]]:
        query = self._build_overpass_query(lat, lon, radius)

        for url in self.OVERPASS_SERVERS:
            data = self._request_overpass(url=url, query=query)

            if data is None:
                continue

            pois = self._parse_pois(data)

            if not pois:
                logger.warning(
                    "No POIs found nearby lat=%s lon=%s radius=%s",
                    lat,
                    lon,
                    radius,
                )

            return pois

        logger.error("Could not get nearby POIs from any Overpass server")
        return []

    def get_location_details(self) -> dict[str, Any]:
        logger.info("Starting location processing...")

        location_address = self.get_address(
            lat=self.narration_settings.latitude,
            lon=self.narration_settings.longitude,
            language_tag=self.narration_settings.language.language_tag,
            zoom_level=self.narration_settings.detail_level,
        )

        points_of_interest = self.get_nearby_pois(
            lat=self.narration_settings.latitude,
            lon=self.narration_settings.longitude,
            radius=self.narration_settings.search_radius,
        )

        logger.info("Finished location processing")

        return {
            "location_address": location_address,
            "points_of_interest": points_of_interest,
        }