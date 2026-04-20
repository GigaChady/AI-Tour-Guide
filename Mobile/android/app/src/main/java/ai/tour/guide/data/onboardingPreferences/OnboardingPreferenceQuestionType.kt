package ai.tour.guide.data.onboardingPreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OnboardingPreferenceQuestionType {
    @SerialName("single_choice")
    SINGLE_CHOICE,

    @SerialName("multi_choice")
    MULTIPLE_CHOICE,
}