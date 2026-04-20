from pathlib import Path

from fastapi import APIRouter, HTTPException
from fastapi.responses import FileResponse

from app.core.audio_store import audio_store

router = APIRouter()

_MEDIA_TYPES: dict[str, str] = {
    ".m3u8": "application/vnd.apple.mpegurl",
    ".ts": "video/mp2t",
}


@router.get("/audio/{narration_id}/{filename}")
async def get_audio_file(narration_id: str, filename: str) -> FileResponse:
    hls_dir = audio_store.get(narration_id)
    if hls_dir is None:
        raise HTTPException(status_code=404, detail="not found")

    # Resolve paths to prevent directory traversal
    file_path = (hls_dir / filename).resolve()
    hls_dir_resolved = hls_dir.resolve()
    try:
        file_path.relative_to(hls_dir_resolved)
    except ValueError:
        raise HTTPException(status_code=404, detail="not found")

    if not file_path.is_file():
        raise HTTPException(status_code=404, detail="not found")

    media_type = _MEDIA_TYPES.get(file_path.suffix.lower(), "application/octet-stream")
    return FileResponse(file_path, media_type=media_type)
