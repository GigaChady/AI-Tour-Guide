package ai.tour.guide.ui.screens.onboarding.auth

import ai.tour.guide.R
import ai.tour.guide.config.AppConfig
import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.data.state.BaseViewModel
import ai.tour.guide.network.ApiClient
import ai.tour.guide.network.ApiClientRoute
import ai.tour.guide.network.schema.request.GoogleTokenRequestDto
import ai.tour.guide.network.schema.request.LoginRequestDto
import ai.tour.guide.network.schema.request.RegisterRequestDto
import ai.tour.guide.network.schema.response.TokenResponseDto
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel


@KoinViewModel
class OnboardingAuthStepViewModel(
    private val apiClient: ApiClient,
    private val appStateRepository: AppDataRepository
) : BaseViewModel<OnboardingAuthStepState>(OnboardingAuthStepState.default()) {
    private val tag = "OnboardingAuthScreenViewModel"

    fun onLoginClicked() {
        viewModelScope.launch {
            withLoading {
                val data = LoginRequestDto(
                    email = viewStateFlow.value.data.email,
                    password = viewStateFlow.value.data.password,
                )
                val response =
                    apiClient.post<LoginRequestDto, TokenResponseDto>(
                        ApiClientRoute.AUTH_LOGIN,
                        data
                    )
                if (!response.isSuccessful) {
                    updateState {
                        copy(errorMessage = response.errorMessage)
                    }
                    return@withLoading
                }
                updateAppConfig(response.body)
                updateState { copy(errorMessage = null, isSuccess = true) }
            }
        }
    }

    fun onRegisterClicked() {
        val password = viewStateFlow.value.data.password
        val confirmPassword = viewStateFlow.value.data.confirmPassword
        if (password != confirmPassword) {
            updateState {
                copy(errorMessage = R.string.validation_error_password_mismatch)
            }
            return
        }
        viewModelScope.launch {
            withLoading {
                val data = RegisterRequestDto(
                    name = viewStateFlow.value.data.name,
                    email = viewStateFlow.value.data.email,
                    password = viewStateFlow.value.data.password,
                )
                val response =
                    apiClient.post<RegisterRequestDto, TokenResponseDto>(
                        ApiClientRoute.AUTH_REGISTER,
                        data
                    )
                if (!response.isSuccessful) {
                    updateState {
                        copy(errorMessage = response.errorMessage)
                    }
                    return@withLoading
                }
                updateAppConfig(response.body)
                updateState { copy(errorMessage = null, isSuccess = true) }
            }
        }
    }

    private fun updateAppConfig(response: TokenResponseDto?) {
        viewModelScope.launch {
            appStateRepository.updateCredentialsWithAPIResponse(response)
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

        loginWithGoogleToken(googleIdTokenCredential.idToken)
    }

    private suspend fun loginWithGoogleToken(googleToken: String) {
        val data = GoogleTokenRequestDto(
            googleToken = googleToken
        )
        val response =
            apiClient.post<GoogleTokenRequestDto, TokenResponseDto>(
                ApiClientRoute.AUTH_GOOGLE,
                data
            )
        if (!response.isSuccessful) {
            updateState {
                copy(errorMessage = response.errorMessage)
            }
            return
        }
        updateAppConfig(response.body)
        updateState { copy(errorMessage = null, isSuccess = true) }
    }

    fun onSignInWithGoogleClicked(context: Context) {
        viewModelScope.launch {
            withLoading {
                handleGoogleSignInClicked(context)
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
