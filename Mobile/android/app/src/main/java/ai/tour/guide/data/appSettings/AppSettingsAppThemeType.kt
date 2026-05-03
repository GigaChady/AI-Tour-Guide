package ai.tour.guide.data.appSettings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AppSettingsAppThemeType {
    @SerialName("system")
    SYSTEM,

    @SerialName("light")
    LIGHT,

    @SerialName("dark")
    DARK
}