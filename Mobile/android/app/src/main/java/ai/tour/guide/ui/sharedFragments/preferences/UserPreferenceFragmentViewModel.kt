package ai.tour.guide.ui.sharedFragments.preferences

import ai.tour.guide.R
import ai.tour.guide.data.shared.BaseViewModel
import ai.tour.guide.domain.preferences.OnboardingPreferencesService
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class UserPreferenceFragmentViewModel(
    private val onboardingPreferencesService: OnboardingPreferencesService
) : BaseViewModel<UserPreferenceFragmentState>(UserPreferenceFragmentState()) {
    private var hasStarted = false
    val preferencesFlow = onboardingPreferencesService.preferences

    fun onStart() {
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch {
            withLoading {
                onboardingPreferencesService.fetchPreferencesIfEmpty()?.let { fetchedState ->
                    updateData { fetchedState }
                }
            }
        }
    }

    fun onOptionSelected(
        preferenceKey: String,
        optionKey: String,
    ) {
        updateData {
            copy(selectedSingleOptions = selectedSingleOptions + (preferenceKey to optionKey))
        }
    }

    fun onMultipleOptionToggled(
        preferenceKey: String,
        optionKey: String,
    ) {
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

    fun onSavePreferencesClicked() {
        viewModelScope.launch {
            savePreferences()
        }
    }

    suspend fun savePreferences(
    ) {
        try {
            val saveBlock: suspend () -> Unit = {
                val response =
                    onboardingPreferencesService.savePreferences(viewStateFlow.value.data)
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

            withLoading { saveBlock() }
        } catch (_: CancellationException) {
        }
    }
}
