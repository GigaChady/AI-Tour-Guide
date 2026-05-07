package ai.tour.guide.data.appData

import ai.tour.guide.data.appSettings.AppSettingsAppThemeType
import kotlinx.serialization.Serializable

@Serializable
data class PersistedAppData(
    val onboardingCompleted: Boolean = false,
    val refreshToken: String? = null,
    // App theme must be stored locally
    val appTheme: AppSettingsAppThemeType = AppSettingsAppThemeType.SYSTEM
)