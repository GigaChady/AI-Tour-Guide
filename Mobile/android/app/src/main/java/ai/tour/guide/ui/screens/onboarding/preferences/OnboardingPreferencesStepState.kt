package ai.tour.guide.ui.screens.onboarding.preferences

data class OnboardingPreferencesStepState(
    val selectedSingleOptions: Map<String, String> = emptyMap(),
    val selectedMultipleOptions: Map<String, Set<String>> = emptyMap()
)
