package ai.tour.guide.data.appData

import kotlinx.serialization.Serializable

@Serializable
data class PersistedAppData(
    val onboardingCompleted: Boolean = false,
    val refreshToken: String? = null,
)