package ai.tour.guide.data.appSettings

import kotlinx.serialization.Serializable

@Serializable
enum class AppSettingsAppThemeType {
    SYSTEM,
    DARK,
    LIGHT
}