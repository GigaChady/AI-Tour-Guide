import logging

from AI.narration.narration_manager import NarrationManager
from AI.narration.common.narration_settings import NarrationSettings, NarrationDetailLevel, NarrationLanguage

if __name__ == "__main__":
    # Logging config (debug)
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s [%(levelname)s] (%(funcName)s): %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )

    # Narration settings
    narration_settings = NarrationSettings(
        latitude=41.889799,
        longitude=12.491015,
        detail_level=NarrationDetailLevel.DETAILED,
        search_radius=50,
        language=NarrationLanguage(language_name="polski", language_tag="pl"),
        user_preferences="history architecture"
    )

    narration_manager = NarrationManager.build_narration_manager(narration_settings)

    # Generate narration
    narration = narration_manager.get_narration()
    print(narration)
