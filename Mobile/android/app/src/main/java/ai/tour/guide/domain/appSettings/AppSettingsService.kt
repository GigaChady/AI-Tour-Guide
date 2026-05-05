package ai.tour.guide.domain.appSettings


import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.data.appSettings.AppSettingsAppThemeType
import ai.tour.guide.data.appSettings.AppSettingsDetailLevelType
import ai.tour.guide.data.appSettings.AppSettingsDto
import ai.tour.guide.data.appSettings.AppSettingsRepository
import ai.tour.guide.network.rest.ApiBaseResponseResult
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.rest.ApiClientRoute
import ai.tour.guide.network.schema.request.SaveAppSettingsRequestDto
import ai.tour.guide.network.schema.response.AppSettingsResponseDto
import ai.tour.guide.network.schema.response.EmptyAPIResponse
import ai.tour.guide.ui.screens.main.appSettings.AppSettingsState
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single

@Single
class AppSettingsService(
    private val appSettingsRepository: AppSettingsRepository,
    private val appDataRepository: AppDataRepository,
    private val apiClient: ApiClient
) {

    suspend fun fetchSettingsIfEmpty(): AppSettingsState? {
        val localTheme = appDataRepository.appThemeFlow.first()
        return appSettingsRepository.fetchSettingsIfEmpty()?.toDto(localTheme)?.toState()
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

    // AppSettingsResponseDto needs conversion to AppSettingsDto before further usage
    private fun AppSettingsResponseDto.toDto(currentTheme: AppSettingsAppThemeType? = null): AppSettingsDto {
        return AppSettingsDto(
            appTheme = currentTheme, // Added from local storage
            language = this.language,
            pitch = this.pitch,
            speed = this.speed,
            detailLevel = this.detailLevel,
            autoPlay = this.autoPlay
        )
    }

    private fun AppSettingsDto.toState(): AppSettingsState {
        return AppSettingsState(
            appTheme = this.appTheme ?: AppSettingsAppThemeType.SYSTEM,
            language = this.language ?: "en", // TODO: Get available narration languages (tags + names) from backend API
            pitch = this.pitch?.toFloat() ?: 50f,
            speed = this.speed?.toFloat() ?: 5f,
            detailLevel = this.detailLevel ?: AppSettingsDetailLevelType.MEDIUM,
            autoPlay = this.autoPlay ?: true
        )
    }

    private fun AppSettingsState.toRequestDto(): SaveAppSettingsRequestDto {
        return SaveAppSettingsRequestDto(
            language = this.language,
            pitch = this.pitch.toInt(),
            speed = this.speed.toInt(),
            detailLevel = this.detailLevel,
            autoPlay = this.autoPlay
        )
    }
}