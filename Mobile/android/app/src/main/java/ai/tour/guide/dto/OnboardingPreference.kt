package ai.tour.guide.dto

data class OnboardingPreference(
    val title: String,
    val category: OnboardingPreferenceCategory,
    val body: String? = null,
    val trailingContent: String? = null,
    val type: OnboardingPreferenceChoiceType = OnboardingPreferenceChoiceType.SINGLE
)
