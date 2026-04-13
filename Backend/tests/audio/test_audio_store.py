from unittest.mock import patch
import pytest

from app.core.audio_store import AudioStore


def test_register_and_get_returns_path(tmp_path):
    store = AudioStore()
    store.register("abc", tmp_path)
    assert store.get("abc") == tmp_path


def test_get_unknown_returns_none():
    store = AudioStore()
    assert store.get("nonexistent") is None


def test_cleanup_expired_removes_old_entries(tmp_path):
    store = AudioStore()
    store.register("old", tmp_path)

    with patch("app.core.audio_store.time") as mock_time:
        mock_time.monotonic.return_value = 9999.0
        with patch("app.core.audio_store.shutil.rmtree") as mock_rmtree:
            store.cleanup_expired(ttl_seconds=600)
            mock_rmtree.assert_called_once_with(tmp_path, ignore_errors=True)

    assert store.get("old") is None


def test_cleanup_expired_keeps_fresh_entries(tmp_path):
    store = AudioStore()
    store.register("fresh", tmp_path)

    with patch("app.core.audio_store.shutil.rmtree") as mock_rmtree:
        store.cleanup_expired(ttl_seconds=600)
        mock_rmtree.assert_not_called()

    assert store.get("fresh") == tmp_path
