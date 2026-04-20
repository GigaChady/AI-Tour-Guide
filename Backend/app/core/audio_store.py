import shutil
import time
from dataclasses import dataclass, field
from pathlib import Path


@dataclass
class _AudioEntry:
    path: Path
    created_at: float = field(default_factory=time.monotonic)


class AudioStore:
    def __init__(self) -> None:
        self._store: dict[str, _AudioEntry] = {}

    def register(self, narration_id: str, path: Path) -> None:
        self._store[narration_id] = _AudioEntry(path=path)

    def get(self, narration_id: str) -> Path | None:
        entry = self._store.get(narration_id)
        return entry.path if entry else None

    def cleanup_expired(self, ttl_seconds: int) -> None:
        now = time.monotonic()
        expired = [
            nid for nid, entry in self._store.items()
            if now - entry.created_at > ttl_seconds
        ]
        for nid in expired:
            shutil.rmtree(self._store[nid].path, ignore_errors=True)
            del self._store[nid]


audio_store = AudioStore()
