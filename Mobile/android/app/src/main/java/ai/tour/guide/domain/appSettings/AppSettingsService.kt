package ai.tour.guide.domain.appSettings


import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.data.appSettings.AppSettingsRepository
import ai.tour.guide.data.appSettings.toState
import ai.tour.guide.network.rest.ApiBaseResponseResult
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.rest.ApiClientRoute
import ai.tour.guide.network.schema.request.SaveAppSettingsRequestDto
import ai.tour.guide.network.schema.response.EmptyAPIResponse
import ai.tour.guide.network.schema.response.toDto
import ai.tour.guide.ui.screens.main.appSettings.AppSettingsState
import ai.tour.guide.ui.screens.main.appSettings.toRequestDto
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
}