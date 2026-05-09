package ai.tour.guide.ui.screens.main.appSettings

import ai.tour.guide.R
import ai.tour.guide.data.appSettings.AppSettingsAppThemeType
import ai.tour.guide.data.appSettings.AppSettingsDetailLevelType
import ai.tour.guide.data.shared.BaseViewModel
import ai.tour.guide.domain.appSettings.AppSettingsService
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class AppSettingsViewModel(
    private val appSettingsService: AppSettingsService
) : BaseViewModel<AppSettingsState>(AppSettingsState()) {
    private var hasStarted = false

    fun onStart() {
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch {
            withLoading {
                appSettingsService.fetchSettingsIfEmpty()?.let { fetchedState ->
                    updateData { fetchedState }
                }
            }
        }
    }

    fun updateTheme(theme: AppSettingsAppThemeType) = updateData { copy(appTheme = theme) }
    fun updateLanguage(lang: String) = updateData { copy(language = lang) }
    fun updatePitch(pitch: Float) = updateData { copy(pitch = pitch) }
    fun updateSpeed(speed: Float) = updateData { copy(speed = speed) }
    fun updateDetailLevel(level: AppSettingsDetailLevelType) = updateData { copy(detailLevel = level) }
    fun updateAutoPlay(autoPlay: Boolean) = updateData { copy(autoPlay = autoPlay) }

    fun onSaveSettingsClicked() {
        viewModelScope.launch {
            saveSettings()
        }
    }

    private suspend fun saveSettings() {
        withLoading {
            val response = appSettingsService.saveSettings(viewStateFlow.value.data)

            if (response.isSuccessful) {
                updateState {
                    copy(
                        toastMessage = R.string.settings_saved_success,
                        isSuccess = true
                    )
                }
            } else {
                updateState { copy(toastMessage = response.errorMessage) }
            }
        }
    }
}