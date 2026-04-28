import json
import logging
import os
from io import BytesIO
from minio import Minio
from minio.error import S3Error

logger = logging.getLogger(__name__)


class MinioImageStorage:
    # TODO: add abstract layer for image storage and make this a concrete implementation.
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
        self._ensure_public_read_policy()

    def _ensure_bucket_exists(self):
        if not self.client.bucket_exists(self.bucket):
            self.client.make_bucket(self.bucket)

    def _ensure_public_read_policy(self):
        """
        Sets a bucket policy to allow public read access to all objects in the bucket.
        TODO: In production, consider more restrictive policies and proper authentication instead of public access.
        """
        policy = {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Principal": {"AWS": ["*"]},
                    "Action": ["s3:GetObject"],
                    "Resource": [f"arn:aws:s3:::{self.bucket}/*"],
                }
            ],
        }

        self.client.set_bucket_policy(self.bucket, json.dumps(policy))

    def object_exists(self, object_name: str) -> bool:
        try:
            self.client.stat_object(self.bucket, object_name)
            return True
        except S3Error as e:
            if e.code == "NoSuchKey":
                return False
            raise

    def build_public_url(self, object_name: str) -> str:
        protocol = "https" if self.secure else "http"
        return f"{protocol}://{self.public_endpoint}/{self.bucket}/{object_name}"

    def upload_bytes(
        self,
        object_name: str,
        data: bytes,
        content_type: str = "image/jpeg",
        overwrite: bool = False,
    ) -> dict:
        if not overwrite and self.object_exists(object_name):
            logger.info("Object %s already exists in bucket %s, skipping upload", object_name, self.bucket)
            return {
                "bucket": self.bucket,
                "image_key": object_name,
                "image_url": self.build_public_url(object_name),
                "already_exists": True,
            }

        self.client.put_object(
            bucket_name=self.bucket,
            object_name=object_name,
            data=BytesIO(data),
            length=len(data),
            content_type=content_type,
        )
        logger.info("Uploaded %s to bucket %s", object_name, self.bucket)

        return {
            "bucket": self.bucket,
            "image_key": object_name,
            "image_url": self.build_public_url(object_name),
            "already_exists": False,
        }