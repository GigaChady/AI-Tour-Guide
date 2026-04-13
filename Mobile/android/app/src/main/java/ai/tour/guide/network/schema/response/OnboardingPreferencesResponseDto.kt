package ai.tour.guide.network.schema.response

import ai.tour.guide.data.onboardingPreferences.OnboardingPreferencesDto
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingPreferencesResponseDto(
    val preferences: List<OnboardingPreferencesDto> = emptyList(),
    override val detail: String? = null
) : IAPIResponseDto
