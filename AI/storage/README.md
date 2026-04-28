# MinIO image storage

This module uploads POI images to MinIO and returns public URLs.

## Environment variables

- `MINIO_ENDPOINT` (default: `localhost:9000`)
- `MINIO_PUBLIC_ENDPOINT` (default: `MINIO_ENDPOINT`)
- `MINIO_ACCESS_KEY` (default: `admin`)
- `MINIO_SECRET_KEY` (default: `admin12345`)
- `MINIO_BUCKET` (default: `poi-images`)
- `MINIO_SECURE` (default: `false`)

## Usage

```python
from storage.minio_image_storage import MinioImageStorage

storage = MinioImageStorage()
result = storage.upload_bytes("example.jpg", b"fake-bytes", content_type="image/jpeg")
print(result["image_url"])
```

## Smoke test

Run from `AI/` after starting MinIO in Docker.

```bash
python storage/minio_smoke_test.py
```

