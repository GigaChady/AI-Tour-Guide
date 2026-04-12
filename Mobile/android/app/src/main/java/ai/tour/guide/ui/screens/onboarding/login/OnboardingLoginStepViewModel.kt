package ai.tour.guide.ui.screens.onboarding.login

import ai.tour.guide.config.AppConfig
import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.network.ApiClient
import ai.tour.guide.network.ApiClientRoute
import ai.tour.guide.network.schema.request.LoginRequest
import ai.tour.guide.network.schema.response.TokenResponse
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.KoinViewModel


@KoinViewModel
class OnboardingLoginStepViewModel(
    private val apiClient: ApiClient,
    private val appStateRepository: AppDataRepository
) : ViewModel() {
    private val tag = "OnboardingLoginStepViewModel"
    private val _state = MutableStateFlow(OnboardingLoginStepState.default())
    val stateFlow: StateFlow<OnboardingLoginStepState> = _state.asStateFlow()
    private val stateLock = Mutex()

    fun onLoginClicked() {
        viewModelScope.launch {
            withLoading {
                val data = LoginRequest(
                    email = stateFlow.value.email,
                    password = stateFlow.value.password,
                )
                val response =
                    apiClient.post<LoginRequest, TokenResponse>(ApiClientRoute.AUTH_LOGIN, data)
                if (!response.isSuccessful) {
                    updateState {
                        copy(errorMessage = response.body?.detail)
                    }
                } else {
                    appStateRepository.updateRefreshToken(response.body?.refreshToken)
                    updateState { copy(errorMessage = null) }
                }
            }
        }
    }

    private suspend fun handleGoogleSignInClicked(context: Context) {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption =
            GetSignInWithGoogleOption.Builder(AppConfig.SIGN_IN_WITH_GOOGLE_CLIENT_ID)
                .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = credentialManager.getCredential(context, request)
        if (response.credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            Log.w(tag, "Credential is not of type Google ID!")
            return
        }

        val googleIdTokenCredential =
            GoogleIdTokenCredential.createFrom(response.credential.data)
        Log.i(tag, googleIdTokenCredential.idToken)
    }

    fun onSignInWithGoogleClicked(context: Context) {
        viewModelScope.launch {
            withLoading {
                handleGoogleSignInClicked(context)
            }
        }
    }

    private suspend fun withLoading(block: suspend () -> Unit) {
        updateState { copy(isLoading = true, errorMessage = null) }
        try {
            block()
        } catch (e: Exception) {
            updateState { copy(errorMessage = e.message) }
        } finally {
            updateState { copy(isLoading = false) }
        }
    }


    fun clearError() {
        updateState {
            copy(errorMessage = null)
        }
    }

    fun onEmailChanged(newEmail: String) {
        updateState {
            copy(email = newEmail)
        }
    }

    fun onPasswordChanged(newPassword: String) {
        updateState {
            copy(password = newPassword)
        }
    }

    private fun updateState(updater: OnboardingLoginStepState.() -> OnboardingLoginStepState) {
        viewModelScope.launch {
            stateLock.withLock {
                _state.value = _state.value.updater()
            }
        }
    }
}
