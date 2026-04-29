import logging
import os

from narration.narration_manager import NarrationManager
from utils.schemas import NarrationSettings, NarrationDetailLevel, NarrationLanguage
from connections.redis_stream_worker import RedisStreamWorker

if __name__ == "__main__":
    # Logging config (debug)
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s [%(levelname)s] (%(funcName)s): %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )

    if os.getenv("AI_RUN_STREAM_WORKER", "1") == "1":


        stream_worker = RedisStreamWorker()
        run_worker()
        
    else:
        
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
        #narration = narration_manager.get_narration()
        print("Mock Narration - Colosseum in Rome, Italy: The Colosseum, also known as the Flavian Amphitheatre, is an iconic symbol of ancient Rome. Built between 70-80 AD, it was used for gladiatorial contests and public spectacles. With a capacity of around 50,000 spectators, it remains one of the greatest architectural and engineering feats of the Roman Empire.")
