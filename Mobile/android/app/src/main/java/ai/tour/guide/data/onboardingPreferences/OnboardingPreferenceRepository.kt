package ai.tour.guide.data.onboardingPreferences


import ai.tour.guide.network.ApiClient
import ai.tour.guide.network.ApiClientRoute
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single
class OnboardingPreferenceRepository(private val apiClient: ApiClient) {

    private val _preferences = MutableStateFlow<List<OnboardingPreferencesDto>>(emptyList())
    val preferences: StateFlow<List<OnboardingPreferencesDto>> = _preferences.asStateFlow()

    private suspend fun fetchPreferences() {
        val request =
            apiClient.getList<OnboardingPreferencesDto>(ApiClientRoute.ONBOARDING_QUESTIONS)
        if (request.isSuccessful) {
            _preferences.value = request.body ?: emptyList()
        } else {
            Log.e(
                "OnboardingPreferenceRepo",
                "Failed to fetch preferences: ${request.errorMessage}"
            )
        }
    }

    suspend fun fetchPreferencesIfEmpty() {
        if (_preferences.value.isEmpty()) {
            fetchPreferences()
        }
    }
}