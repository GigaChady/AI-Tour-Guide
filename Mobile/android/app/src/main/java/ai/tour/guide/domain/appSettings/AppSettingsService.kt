package ai.tour.guide.domain.appSettings


import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.data.appSettings.AppSettingsAppThemeType
import ai.tour.guide.data.appSettings.AppSettingsDetailLevelType
import ai.tour.guide.data.appSettings.AppSettingsRepository
import ai.tour.guide.network.rest.ApiBaseResponseResult
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.rest.ApiClientRoute
import ai.tour.guide.network.schema.request.SaveAppSettingsRequestDto
import ai.tour.guide.network.schema.response.AppSettingsResponseDto
import ai.tour.guide.network.schema.response.EmptyAPIResponse
import ai.tour.guide.ui.screens.main.appSettings.AppSettingsState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single

@Single
class AppSettingsService(
    private val appSettingsRepository: AppSettingsRepository,
    private val appDataRepository: AppDataRepository,
    private val apiClient: ApiClient
) {
    val settingsFlow: StateFlow<AppSettingsResponseDto?>
        get() = appSettingsRepository.settings

    suspend fun fetchSettingsIfEmpty(): AppSettingsState {
        val localTheme = appDataRepository.appThemeFlow.first()
        val apiSettings = appSettingsRepository.settings.value ?: appSettingsRepository.fetchSettingsIfEmpty()
        // Merge local and API settings
        return apiSettings?.toState(currentTheme = localTheme)
            ?: AppSettingsState(appTheme = localTheme)
    }

    suspend fun saveSettings(state: AppSettingsState): ApiBaseResponseResult {
        // Save app theme locally
        appDataRepository.updateAppTheme(state.appTheme)

        // Send settings to backend API
        val requestDto = state.toRequestDto()
        return apiClient.post<SaveAppSettingsRequestDto, EmptyAPIResponse>(
            ApiClientRoute.USER_NARRATION_SETTINGS,
            requestDto
        )
    }

    private fun AppSettingsResponseDto.toState(currentTheme: AppSettingsAppThemeType): AppSettingsState {
        return AppSettingsState(
            appTheme = currentTheme,
            language = this.language ?: "pl", // TODO: Get available narration languages from backend API
            pitch = this.pitch?.toFloat() ?: 50f,
            speed = this.speed?.toFloat() ?: 5f,
            volume = this.volume?.toFloat() ?: 5f,
            detailLevel = this.detailLevel ?: AppSettingsDetailLevelType.MEDIUM,
            autoPlay = this.autoPlay ?: true
        )
    }

    private fun AppSettingsState.toRequestDto(): SaveAppSettingsRequestDto {
        return SaveAppSettingsRequestDto(
            language = this.language,
            pitch = this.pitch.toInt(),
            speed = this.speed.toInt(),
            volume = this.volume.toInt(),
            detailLevel = this.detailLevel,
            autoPlay = this.autoPlay
        )
    }
}