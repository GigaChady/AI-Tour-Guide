from __future__ import annotations

import json
import logging
from typing import Any

try:
    from unidecode import unidecode
except ImportError:
    def unidecode(value: str) -> str:
        return value

logger = logging.getLogger(__name__)


class NarrationResponseParser:
    def parse(self, content: str) -> dict[str, Any] | str:
        try:
            result = json.loads(content)
        except json.JSONDecodeError as e:
            logger.error("Invalid narration response. JSON decoding error: %s", e)
            logger.error("Raw response: %s", content)
            return unidecode(content)

        if isinstance(result, dict):
            for key, value in result.items():
                if isinstance(value, str):
                    result[key] = unidecode(value)

        logger.info("Narration with ASCII conversion: %s", result)
        return result
