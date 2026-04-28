import os
import time

from storage.minio_image_storage import MinioImageStorage


def main() -> int:
    storage = MinioImageStorage()

    object_name = f"smoke-test-{int(time.time())}.jpg"
    data = b"minio-smoke-test"

    result = storage.upload_bytes(object_name, data, content_type="image/jpeg")

    print("Uploaded")
    print(f"bucket={result['bucket']}")
    print(f"image_key={result['image_key']}")
    print(f"image_url={result['image_url']}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

