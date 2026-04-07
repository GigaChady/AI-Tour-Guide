import pytest


def test_factory_returns_edge_provider(monkeypatch):
    monkeypatch.setenv("TTS_PROVIDER", "edge")
    import importlib
    import app.core.config as cfg_module
    importlib.reload(cfg_module)
    from app.services.tts.factory import TTSFactory
    from app.services.tts.edge import EdgeTTSProvider
    provider = TTSFactory.get_provider()
    assert isinstance(provider, EdgeTTSProvider)


def test_factory_raises_for_unknown_provider(monkeypatch):
    monkeypatch.setenv("TTS_PROVIDER", "banana")
    import importlib
    import app.core.config as cfg_module
    importlib.reload(cfg_module)
    from app.services.tts.factory import TTSFactory
    with pytest.raises(ValueError, match="Unknown TTS provider: banana"):
        TTSFactory.get_provider()
