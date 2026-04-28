import os
from io import BytesIO

from minio import Minio
from minio.error import S3Error


class MinioImageStorage:
    def __init__(self):
        self.endpoint = os.getenv("MINIO_ENDPOINT", "localhost:9000")
        self.public_endpoint = os.getenv("MINIO_PUBLIC_ENDPOINT", self.endpoint)
        self.access_key = os.getenv("MINIO_ACCESS_KEY", "admin")
        self.secret_key = os.getenv("MINIO_SECRET_KEY", "admin12345")
        self.bucket = os.getenv("MINIO_BUCKET", "poi-images")
        self.secure = os.getenv("MINIO_SECURE", "false").lower() == "true"

        self.client = Minio(
            endpoint=self.endpoint,
            access_key=self.access_key,
            secret_key=self.secret_key,
            secure=self.secure,
        )

        self._ensure_bucket_exists()

    def _ensure_bucket_exists(self):
        if not self.client.bucket_exists(self.bucket):
            self.client.make_bucket(self.bucket)

    def upload_bytes(
        self,
        object_name: str,
        data: bytes,
        content_type: str = "image/jpeg",
    ) -> dict:
        try:
            self.client.put_object(
                bucket_name=self.bucket,
                object_name=object_name,
                data=BytesIO(data),
                length=len(data),
                content_type=content_type,
            )

            protocol = "https" if self.secure else "http"
            public_url = f"{protocol}://{self.public_endpoint}/{self.bucket}/{object_name}"

            return {
                "bucket": self.bucket,
                "image_key": object_name,
                "image_url": public_url,
            }

        except S3Error as e:
            raise RuntimeError(f"Could not upload image to MinIO: {e}") from e

