package ai.tour.guide.domain.preferences

import ai.tour.guide.data.onboardingPreferences.OnboardingPreferenceQuestionType
import ai.tour.guide.data.onboardingPreferences.OnboardingPreferenceRepository
import ai.tour.guide.data.onboardingPreferences.OnboardingPreferencesDto
import ai.tour.guide.network.ApiBaseResponseResult
import ai.tour.guide.network.ApiClient
import ai.tour.guide.network.ApiClientRoute
import ai.tour.guide.network.schema.request.OnboardingPreferenceToSave
import ai.tour.guide.network.schema.request.SaveOnboardingPreferenceRequestDto
import ai.tour.guide.network.schema.response.EmptyAPIResponse
import ai.tour.guide.network.schema.response.OnboardingPreferencesResponseDto
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

    suspend fun fetchPreferencesIfEmpty(): UserPreferenceFragmentState? =
        onboardingPreferenceRepository.fetchPreferencesIfEmpty()?.toUserPreferenceState()

    suspend fun savePreferences(state: UserPreferenceFragmentState): ApiBaseResponseResult {
        val data = parseStateToDto(state)
        return apiClient.post<SaveOnboardingPreferenceRequestDto, EmptyAPIResponse>(
            ApiClientRoute.ONBOARDING_ANSWERS,
            data
        )
    }

    private fun parseStateToDto(state: UserPreferenceFragmentState): SaveOnboardingPreferenceRequestDto {
        val singleRequests = state.selectedSingleOptions
            .filterValues { it.isNotBlank() }
            .map { (questionKey, answerKey) ->
                OnboardingPreferenceToSave(questionKey = questionKey, answerKey = answerKey)
            }
        val multipleRequests = state.selectedMultipleOptions
            .mapValues { (_, answerKeys) -> answerKeys.filter { it.isNotBlank() } }
            .filterValues { it.isNotEmpty() }
            .map { (questionKey, answerKeys) ->
                OnboardingPreferenceToSave(
                    questionKey = questionKey,
                    answerKeys = answerKeys
                )
            }
        return SaveOnboardingPreferenceRequestDto(singleRequests + multipleRequests)
    }

    private fun OnboardingPreferencesResponseDto.toUserPreferenceState(): UserPreferenceFragmentState {
        val singleSelections = mutableMapOf<String, String>()
        val multipleSelections = mutableMapOf<String, Set<String>>()

        items.forEach { preference ->
            val questionKey = preference.key ?: return@forEach
            when (preference.type) {
                OnboardingPreferenceQuestionType.SINGLE_CHOICE -> {
                    getSelectedAnswer(questionKey)?.takeIf { it.isNotBlank() }?.let {
                        singleSelections[questionKey] = it
                    }
                }

                OnboardingPreferenceQuestionType.MULTIPLE_CHOICE -> {
                    getSelectedAnswers(questionKey)
                        .filter { it.isNotBlank() }
                        .toSet()
                        .takeIf { it.isNotEmpty() }
                        ?.let { multipleSelections[questionKey] = it }
                }

                null -> Unit
            }
        }

        return UserPreferenceFragmentState(
            selectedSingleOptions = singleSelections,
            selectedMultipleOptions = multipleSelections
        )
    }
}
