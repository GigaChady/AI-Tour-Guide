import pytest
from app.services.tts.chunker import split


def test_split_on_period():
    result = split("Hello. Welcome.")
    assert result == [(0, "Hello"), (1, "Welcome")]


def test_split_on_exclamation():
    result = split("Stop! Go!")
    assert result == [(0, "Stop"), (1, "Go")]


def test_split_on_question():
    result = split("What? Really?")
    assert result == [(0, "What"), (1, "Really")]


def test_split_on_comma():
    result = split("One, two, three.")
    assert result == [(0, "One"), (1, "two"), (2, "three")]


def test_split_skips_empty_chunks():
    result = split("Hello... World")
    assert result == [(0, "Hello"), (1, "World")]


def test_split_strips_whitespace():
    result = split("  Hello.   World.  ")
    assert result == [(0, "Hello"), (1, "World")]


def test_split_empty_string():
    assert split("") == []


def test_split_ids_are_sequential():
    result = split("A. B. C.")
    ids = [chunk_id for chunk_id, _ in result]
    assert ids == [0, 1, 2]


def test_split_single_chunk_no_punctuation():
    result = split("Hello world")
    assert result == [(0, "Hello world")]


def test_split_only_delimiters():
    assert split("...") == []


def test_split_mixed_consecutive_delimiters():
    result = split("Hello!? World")
    assert result == [(0, "Hello"), (1, "World")]


def test_split_leading_and_trailing_delimiters():
    result = split(". Hello. World.")
    assert result == [(0, "Hello"), (1, "World")]
