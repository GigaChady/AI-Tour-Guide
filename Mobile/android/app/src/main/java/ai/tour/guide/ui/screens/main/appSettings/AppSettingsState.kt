package ai.tour.guide.ui.screens.main.appSettings

import ai.tour.guide.data.appSettings.AppSettingsAppThemeType
import ai.tour.guide.data.appSettings.AppSettingsDetailLevelType


data class AppSettingsState(
    val appTheme: AppSettingsAppThemeType = AppSettingsAppThemeType.SYSTEM,
    val language: String = "pl",
    val pitch: Float = 50f,
    val speed: Float = 50f,
    val volume: Float = 20f, // Not used in View (AppSettingsScreen) TODO: Decide whether this option should be kept
    val detailLevel: AppSettingsDetailLevelType = AppSettingsDetailLevelType.MEDIUM,
    val autoPlay: Boolean = true
) {
    companion object {
        fun default() = AppSettingsState()
    }
}