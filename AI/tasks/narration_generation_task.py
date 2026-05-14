from __future__ import annotations

from agents.contracts import NarrativeGenerationAgent
from schemas import FilteredPoiFacts, NarrationResult
from schemas import NarrationSettings


class NarrationGenerationTask:
    def __init__(self, narrative_generation_agent: NarrativeGenerationAgent):
        self.narrative_generation_agent = narrative_generation_agent

    def run(
        self,
        filtered_facts: FilteredPoiFacts,
        narration_settings: NarrationSettings,
    ) -> NarrationResult:
        return NarrationResult.from_model_response(
            self.narrative_generation_agent.generate_narration(
                location_name=filtered_facts.poi.name,
                location_info=filtered_facts.facts,
            )
        )
