package ai.tour.guide.domain.preferences

import ai.tour.guide.data.onboardingPreferences.OnboardingPreferenceRepository
import ai.tour.guide.data.onboardingPreferences.OnboardingPreferencesDto
import ai.tour.guide.network.ApiBaseResponseResult
import ai.tour.guide.network.ApiClient
import ai.tour.guide.network.ApiClientRoute
import ai.tour.guide.network.schema.request.OnboardingPreferenceToSave
import ai.tour.guide.network.schema.request.SaveOnboardingPreferenceRequestDto
import ai.tour.guide.network.schema.response.EmptyAPIResponse
import ai.tour.guide.ui.sharedFragments.preferences.UserPreferenceFragmentState
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

@Single
class OnboardingPreferencesService(
    private val onboardingPreferenceRepository: OnboardingPreferenceRepository,
    private val apiClient: ApiClient
) {
    val preferences: StateFlow<List<OnboardingPreferencesDto>>
        get() = onboardingPreferenceRepository.preferences

    suspend fun fetchPreferencesIfEmpty() {
        onboardingPreferenceRepository.fetchPreferencesIfEmpty()
    }

    suspend fun savePreferences(state: UserPreferenceFragmentState): ApiBaseResponseResult {
        val data = parseStateToDto(state)
        return apiClient.post<SaveOnboardingPreferenceRequestDto, EmptyAPIResponse>(
            ApiClientRoute.ONBOARDING_ANSWERS,
            data
        )
    }

    private fun parseStateToDto(state: UserPreferenceFragmentState): SaveOnboardingPreferenceRequestDto {
        val singleRequests = state.selectedSingleOptions.map { (questionKey, answerKey) ->
            OnboardingPreferenceToSave(questionKey = questionKey, answerKey = answerKey)
        }
        val multipleRequests = state.selectedMultipleOptions.map { (questionKey, answerKeys) ->
            OnboardingPreferenceToSave(
                questionKey = questionKey,
                answerKeys = answerKeys.toList()
            )
        }
        return SaveOnboardingPreferenceRequestDto(singleRequests + multipleRequests)
    }
}
