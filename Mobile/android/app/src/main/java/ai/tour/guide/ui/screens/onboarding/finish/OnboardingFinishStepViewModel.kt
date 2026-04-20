package ai.tour.guide.ui.screens.onboarding.finish

import ai.tour.guide.data.appData.AppDataRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class OnboardingFinishStepViewModel(private val appDataRepository: AppDataRepository) :
    ViewModel() {
    private val _state = MutableStateFlow<Boolean>(false)
    val completedStateFlow = _state.asStateFlow()

    fun onFinishClicked() {
        viewModelScope.launch {
            appDataRepository.updateOnboardingCompleted(true)
            _state.value = true
        }
    }
}