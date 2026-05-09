package ai.tour.guide.data.appSettings

import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.rest.ApiClientRoute
import ai.tour.guide.network.schema.response.AppSettingsResponseDto
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single
class AppSettingsRepository(private val apiClient: ApiClient) {

    private val _settings = MutableStateFlow<AppSettingsResponseDto?>(null)
    val settings: StateFlow<AppSettingsResponseDto?> = _settings.asStateFlow()

    private suspend fun fetchSettings(): AppSettingsResponseDto? {
        val request = apiClient.get<AppSettingsResponseDto>(ApiClientRoute.USER_NARRATION_SETTINGS)
        if (request.isSuccessful) {
            _settings.value = request.body
            return request.body
        } else {
            Log.e("AppSettingsRepo", "Failed to fetch settings: ${request.errorMessage}")
        }
        return null
    }

    suspend fun fetchSettingsIfEmpty(): AppSettingsResponseDto? {
        if (_settings.value == null) {
            return fetchSettings()
        }
        return null
    }
}