import os
import time
import pytest

import app.core.celery_app as celery_app_module


def _create_file(directory: str, name: str, age_seconds: float) -> str:
    path = os.path.join(directory, name)
    with open(path, "wb") as f:
        f.write(b"data")
    mtime = time.time() - age_seconds
    os.utime(path, (mtime, mtime))
    return path


def test_cleanup_deletes_old_files(tmp_path, monkeypatch):
    monkeypatch.setattr("app.core.config.settings.AUDIO_DIR", str(tmp_path))
    monkeypatch.setattr("app.core.config.settings.AUDIO_FILE_TTL_SECONDS", 3600)

    old_file = _create_file(str(tmp_path), "old.mp3", age_seconds=7200)
    new_file = _create_file(str(tmp_path), "new.mp3", age_seconds=60)

    result = celery_app_module.cleanup_audio.run()

    assert result == {"deleted": 1}
    assert not os.path.exists(old_file)
    assert os.path.exists(new_file)


def test_cleanup_ignores_gitkeep(tmp_path, monkeypatch):
    monkeypatch.setattr("app.core.config.settings.AUDIO_DIR", str(tmp_path))
    monkeypatch.setattr("app.core.config.settings.AUDIO_FILE_TTL_SECONDS", 3600)

    _create_file(str(tmp_path), ".gitkeep", age_seconds=99999)

    result = celery_app_module.cleanup_audio.run()

    assert result == {"deleted": 0}
    assert os.path.exists(os.path.join(str(tmp_path), ".gitkeep"))


def test_cleanup_no_directory(tmp_path, monkeypatch):
    missing_dir = str(tmp_path / "nonexistent")
    monkeypatch.setattr("app.core.config.settings.AUDIO_DIR", missing_dir)

    result = celery_app_module.cleanup_audio.run()

    assert result == {"deleted": 0}
