import pytest
from app.services.tts.chunker import split


def test_split_empty_string():
    assert split("") == []


def test_split_whitespace_only():
    assert split("   ") == []


def test_split_single_chunk_no_punctuation():
    result = split("Hello world")
    assert result == [(0, "Hello world")]


def test_split_strips_leading_trailing_whitespace():
    result = split("  Hello World  ")
    assert result == [(0, "Hello World")]


def test_split_preserves_full_text_with_periods():
    result = split("Hello. Welcome.")
    assert result == [(0, "Hello. Welcome.")]


def test_split_preserves_full_text_with_exclamation():
    result = split("Stop! Go!")
    assert result == [(0, "Stop! Go!")]


def test_split_preserves_full_text_with_question():
    result = split("What? Really?")
    assert result == [(0, "What? Really?")]


def test_split_preserves_full_text_with_comma():
    result = split("One, two, three.")
    assert result == [(0, "One, two, three.")]


def test_split_chunk_id_is_zero():
    result = split("Any text here")
    assert result[0][0] == 0


def test_split_returns_one_chunk():
    result = split("A. B. C.")
    assert len(result) == 1


def test_split_mixed_punctuation_preserved():
    result = split("Hello!? World")
    assert result == [(0, "Hello!? World")]
