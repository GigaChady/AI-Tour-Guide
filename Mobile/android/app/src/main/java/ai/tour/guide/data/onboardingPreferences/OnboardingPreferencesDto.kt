package ai.tour.guide.data.onboardingPreferences

import kotlinx.serialization.Serializable

@Serializable
data class OnboardingPreferencesDto(
    val key: String? = null,
    val title: String? = null,
    val type: OnboardingPreferenceQuestionType? = null,
    val options: List<OnboardingPreferenceQuestionDto>? = null
)