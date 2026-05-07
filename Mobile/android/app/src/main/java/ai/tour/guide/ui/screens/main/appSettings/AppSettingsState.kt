package ai.tour.guide.ui.screens.main.appSettings

import ai.tour.guide.data.appSettings.AppSettingsAppThemeType
import ai.tour.guide.data.appSettings.AppSettingsDetailLevelType
import ai.tour.guide.network.schema.request.SaveAppSettingsRequestDto


data class AppSettingsState(
    val appTheme: AppSettingsAppThemeType = AppSettingsAppThemeType.SYSTEM,
    val language: String = "en",
    val pitch: Float = 50f,
    val speed: Float = 5f,
    val detailLevel: AppSettingsDetailLevelType = AppSettingsDetailLevelType.MEDIUM,
    val autoPlay: Boolean = true
) {
    companion object {
        fun default() = AppSettingsState()
    }
}

fun AppSettingsState.toRequestDto(): SaveAppSettingsRequestDto {
    return SaveAppSettingsRequestDto(
        language = this.language,
        pitch = this.pitch.toInt(),
        speed = this.speed.toInt(),
        detailLevel = this.detailLevel,
        autoPlay = this.autoPlay
    )
}