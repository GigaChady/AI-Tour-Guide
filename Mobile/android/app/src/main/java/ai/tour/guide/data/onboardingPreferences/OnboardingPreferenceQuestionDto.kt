package ai.tour.guide.data.onboardingPreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingPreferenceQuestionDto(
    val key: String? = null,
    val title: String? = null,
    val body: String? = null,
    @SerialName("trailing_content")
    val trailingContent: String? = null
)