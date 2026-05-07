package ai.tour.guide.data.appSettings

import ai.tour.guide.ui.screens.main.appSettings.AppSettingsState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppSettingsDto(

    @SerialName("app_theme")
    val appTheme: AppSettingsAppThemeType? = null,

    val language: String? = null,

    val pitch: Int? = null,

    val speed: Int? = null,

    @SerialName("detail_level")
    val detailLevel: AppSettingsDetailLevelType? = null,

    @SerialName("auto_play")
    val autoPlay: Boolean? = null
)

fun AppSettingsDto.toState(): AppSettingsState {
    return AppSettingsState(
        appTheme = this.appTheme ?: AppSettingsAppThemeType.SYSTEM,
        language = this.language ?: "en", // TODO: Get available narration languages (tags + names) from backend API
        pitch = this.pitch?.toFloat() ?: 50f,
        speed = this.speed?.toFloat() ?: 5f,
        detailLevel = this.detailLevel ?: AppSettingsDetailLevelType.MEDIUM,
        autoPlay = this.autoPlay ?: true
    )
}