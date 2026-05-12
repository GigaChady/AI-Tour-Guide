import logging
import os
import json

from pipeline.tour_narration_pipeline_factory import TourNarrationPipelineFactory
from processors.narration_processor import NarrationProcessor
from processors.photo_processor import PhotoProcessor
from storage.minio_image_storage import MinioImageStorage
from connections.redis_stream_worker import RedisStreamWorker
from processors.backend_processor import BackendProcessor
from schemas import NarrationDetailLevel, NarrationLanguage, NarrationSettings

if __name__ == "__main__":
    # Logging config (debug)
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s [%(levelname)s] (%(funcName)s): %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )

    if os.getenv("AI_RUN_STREAM_WORKER", "1") == "1":

        photo_processor = PhotoProcessor(MinioImageStorage())
        narration_processor = NarrationProcessor(photo_processor=photo_processor)
        backend_processor = BackendProcessor(narration_processor=narration_processor)
        stream_worker = RedisStreamWorker(backend_processor)
        stream_worker.run()
        
    else:
        narration_settings = NarrationSettings(
            latitude=41.889799,
            longitude=12.491015,
            detail_level=NarrationDetailLevel.DETAILED,
            search_radius=50,
            language=NarrationLanguage(language_name="Polish", language_tag="pl"),
            user_preferences="history architecture",
        )

        result = TourNarrationPipelineFactory().create(narration_settings).run()
        print(json.dumps(result.model_dump(), ensure_ascii=False, indent=2))
