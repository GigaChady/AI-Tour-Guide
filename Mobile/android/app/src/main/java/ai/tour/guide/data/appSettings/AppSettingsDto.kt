package ai.tour.guide.data.appSettings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppSettingsDto(
    @SerialName("app_theme")
    val appTheme: AppSettingsAppThemeType? = null,

    @SerialName("language")
    val language: String? = null,

    @SerialName("pitch")
    val pitch: Int? = null,

    @SerialName("speed")
    val speed: Int? = null,

    @SerialName("detail_level")
    val detailLevel: AppSettingsDetailLevelType? = null,

    @SerialName("auto_play")
    val autoPlay: Boolean? = null
)