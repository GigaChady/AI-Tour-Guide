package ai.tour.guide.ui.screens.onboarding.auth

import ai.tour.guide.R
import ai.tour.guide.data.shared.BaseViewModel
import ai.tour.guide.domain.auth.AuthService
import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel


@KoinViewModel
class OnboardingAuthStepViewModel(
    private val authService: AuthService
) : BaseViewModel<OnboardingAuthStepState>(OnboardingAuthStepState.default()) {
    fun onLoginClicked() {
        viewModelScope.launch {
            performLoginRequest()
        }
    }

    suspend fun performLoginRequest() {
        withLoading {
            val response = authService.login(
                email = viewStateFlow.value.data.email,
                password = viewStateFlow.value.data.password
            )
            if (!response.isSuccessful) {
                updateState {
                    copy(toastMessage = response.errorMessage)
                }
                return@withLoading
            }
            updateState { copy(toastMessage = null, isSuccess = true) }
        }
    }

    fun onRegisterClicked() {
        viewModelScope.launch {
            performRegisterRequest()
        }
    }

    suspend fun performRegisterRequest() {
        val password = viewStateFlow.value.data.password
        val confirmPassword = viewStateFlow.value.data.confirmPassword
        if (password != confirmPassword) {
            updateState {
                copy(toastMessage = R.string.validation_error_password_mismatch)
            }
            return
        }
        withLoading {
            val response = authService.register(
                name = viewStateFlow.value.data.name,
                email = viewStateFlow.value.data.email,
                password = viewStateFlow.value.data.password
            )
            if (!response.isSuccessful) {
                updateState {
                    copy(toastMessage = response.errorMessage)
                }
                return@withLoading
            }
            updateState { copy(toastMessage = null, isSuccess = true) }
        }
    }

    fun onSignInWithGoogleClicked(context: Context) {
        viewModelScope.launch {
            withLoading {
                val response = authService.signInWithGoogle(context)
                if (!response.isSuccessful) {
                    updateState {
                        copy(toastMessage = response.errorMessage)
                    }
                    return@withLoading
                }
                updateState { copy(toastMessage = null, isSuccess = true) }
            }
        }
    }

    fun onNameChanged(newName: String) {
        updateData {
            copy(name = newName)
        }
    }

    fun onEmailChanged(newEmail: String) {
        updateData {
            copy(email = newEmail)
        }
    }

    fun onPasswordChanged(newPassword: String) {
        updateData {
            copy(password = newPassword)
        }
    }

    fun onConfirmPasswordChanged(newPassword: String) {
        updateData {
            copy(confirmPassword = newPassword)
        }
    }
}
