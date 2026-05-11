from parsers.narration_response_parser import NarrationResponseParser
from prompts.filtering_prompt_builder import FilteringPromptBuilder
from prompts.narration_prompt_builder import NarrationPromptBuilder


def test_narration_prompt_builder_returns_system_and_user_messages():
    builder = NarrationPromptBuilder()

    messages = builder.build_messages(
        location_name="Town Hall Tower",
        location_info="Historic facts",
        user_preferences="history",
        language_name="Polish",
    )

    assert len(messages) == 2
    assert messages[0][0] == "system"
    assert "valid JSON" in messages[0][1]
    assert "Polish" in messages[0][1]
    assert messages[1][0] == "user"
    assert "Town Hall Tower" in messages[1][1]
    assert "Historic facts" in messages[1][1]


def test_filtering_prompt_builder_keeps_cloud_short_mode_behavior():
    builder = FilteringPromptBuilder()

    prompt = builder.build_cloud_prompt(
        poi_name="Museum",
        poi_description="A detailed museum description",
        user_preferences="art",
        include_prompt=False,
    )

    assert prompt == "User preferences: art"


def test_filtering_prompt_builder_builds_ollama_messages():
    builder = FilteringPromptBuilder()

    messages = builder.build_messages(
        poi_name="Museum",
        raw_text="Raw facts",
        user_preferences="art",
    )

    assert len(messages) == 2
    assert messages[0][0] == "system"
    assert "faktow" in messages[0][1]
    assert messages[1][0] == "user"
    assert "Museum" in messages[1][1]
    assert "Raw facts" in messages[1][1]


def test_narration_response_parser_parses_json_and_normalizes_text():
    parser = NarrationResponseParser()

    result = parser.parse('{"location": "Lodz", "narration": "Short narration"}')

    assert result == {
        "location": "Lodz",
        "narration": "Short narration",
    }


def test_narration_response_parser_returns_text_on_invalid_json():
    parser = NarrationResponseParser()

    result = parser.parse("Narracja poza JSON-em")

    assert result == "Narracja poza JSON-em"
