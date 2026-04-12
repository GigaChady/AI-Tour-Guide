package ai.tour.guide.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PersistedAppData(
    val onboardingCompleted: Boolean = false,
    val refreshToken: String? = null,
)