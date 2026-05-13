package ai.tour.guide.data.onboardingPreferences


import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.rest.ApiClientRoute
import ai.tour.guide.network.schema.response.OnboardingPreferencesResponseDto
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single
import java.util.Locale

@Single
class OnboardingPreferenceRepository(private val apiClient: ApiClient) {

    private val _preferences = MutableStateFlow<List<OnboardingPreferencesDto>>(emptyList())
    val preferences: StateFlow<List<OnboardingPreferencesDto>> = _preferences.asStateFlow()

    private var lastFetchedLang: String? = null

    private suspend fun fetchPreferences(lang: String): OnboardingPreferencesResponseDto? {
        val request = apiClient.get<OnboardingPreferencesResponseDto>(
            ApiClientRoute.USER_ONBOARDING_QUESTIONS,
            queryParams = mapOf("lang" to lang)
        )
        if (request.isSuccessful) {
            _preferences.value = request.body?.items ?: emptyList()
            lastFetchedLang = lang
            return request.body
        } else {
            Log.e(
                "OnboardingPreferenceRepo",
                "Failed to fetch preferences: ${request.errorMessage}"
            )
        }
        return null
    }

    suspend fun fetchPreferencesIfEmpty(): OnboardingPreferencesResponseDto? {
        val lang = Locale.getDefault().language
        if (_preferences.value.isEmpty() || lastFetchedLang != lang) {
            return fetchPreferences(lang)
        }
        return null
    }
}
