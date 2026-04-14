package ai.tour.guide.ui.screens.onboarding.preferences

import ai.tour.guide.data.onboardingPreferences.OnboardingPreferenceRepository
import ai.tour.guide.data.state.BaseViewModel
import ai.tour.guide.network.ApiClient
import ai.tour.guide.network.ApiClientRoute
import ai.tour.guide.network.schema.request.OnboardingPreferenceToSave
import ai.tour.guide.network.schema.request.SaveOnboardingPreferenceRequestDto
import ai.tour.guide.network.schema.response.EmptyAPIResponse
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class OnboardingPreferencesStepViewModel(
    val onboardingPreferenceRepository: OnboardingPreferenceRepository,
    private val apiClient: ApiClient
) : BaseViewModel<OnboardingPreferencesStepState>(OnboardingPreferencesStepState()) {

    val preferencesFlow = onboardingPreferenceRepository.preferences

    fun fetchData() {
        viewModelScope.launch {
            withLoading {
                onboardingPreferenceRepository.fetchPreferencesIfEmpty()
            }
        }
    }

    fun onOptionSelected(preferenceKey: String, optionKey: String) {
        updateData {
            copy(selectedSingleOptions = selectedSingleOptions + (preferenceKey to optionKey))
        }
    }

    fun onMultipleOptionToggled(preferenceKey: String, optionKey: String) {
        updateData {
            val currentSelections = selectedMultipleOptions[preferenceKey] ?: emptySet()
            val newSelections = if (currentSelections.contains(optionKey)) {
                currentSelections - optionKey
            } else {
                currentSelections + optionKey
            }
            copy(selectedMultipleOptions = selectedMultipleOptions + (preferenceKey to newSelections))
        }
    }

    private fun parseStateToDto(): SaveOnboardingPreferenceRequestDto {
        val stateData = viewStateFlow.value.data
        val singleRequests = stateData.selectedSingleOptions.map { (qId, aId) ->
            OnboardingPreferenceToSave(questionKey = qId, answerKey = aId)
        }
        val multipleRequests = stateData.selectedMultipleOptions.map { (qId, aIds) ->
            OnboardingPreferenceToSave(
                questionKey = qId,
                answerKeys = aIds.toList()
            )
        }
        return SaveOnboardingPreferenceRequestDto(singleRequests + multipleRequests)
    }

    fun savePreferences() {
        val data = parseStateToDto()
        viewModelScope.launch {
            withLoading {
                val response =
                    apiClient.post<SaveOnboardingPreferenceRequestDto, EmptyAPIResponse>(
                        ApiClientRoute.ONBOARDING_ANSWERS,
                        data
                    )
                if (response.isSuccessful) {
                    updateState { copy(isSuccess = true) }
                } else {
                    updateState { copy(errorMessage = response.errorMessage) }
                }
            }
        }
    }
}
