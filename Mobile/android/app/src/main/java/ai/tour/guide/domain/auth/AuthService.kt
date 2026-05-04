package ai.tour.guide.domain.auth

import ai.tour.guide.config.AppConfig
import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.network.rest.ApiBaseResponseResult
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.rest.ApiClientRoute
import ai.tour.guide.network.rest.ApiResponse
import ai.tour.guide.network.schema.request.GoogleTokenRequestDto
import ai.tour.guide.network.schema.request.LoginRequestDto
import ai.tour.guide.network.schema.request.RegisterRequestDto
import ai.tour.guide.network.schema.response.MeParamsResponseDto
import ai.tour.guide.network.schema.response.TokenResponseDto
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import org.koin.core.annotation.Single

@Single
class AuthService(
    private val apiClient: ApiClient,
    private val appDataRepository: AppDataRepository
) {
    suspend fun login(email: String, password: String): ApiBaseResponseResult {
        val response = apiClient.post<LoginRequestDto, TokenResponseDto>(
            ApiClientRoute.AUTH_LOGIN,
            LoginRequestDto(
                email = email,
                password = password
            )
        )
        return handleAuthResponse(response)
    }

    suspend fun register(name: String, email: String, password: String): ApiBaseResponseResult {
        val response = apiClient.post<RegisterRequestDto, TokenResponseDto>(
            ApiClientRoute.AUTH_REGISTER,
            RegisterRequestDto(
                name = name,
                email = email,
                password = password
            )
        )
        return handleAuthResponse(response)
    }

    suspend fun signInWithGoogle(context: Context): ApiBaseResponseResult {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption =
            GetSignInWithGoogleOption.Builder(AppConfig.SIGN_IN_WITH_GOOGLE_CLIENT_ID)
                .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        var response: GetCredentialResponse?
        try {
            response = credentialManager.getCredential(context, request)
        } catch (e: NoCredentialException) {
            Log.e(TAG, e.stackTraceToString())
            return FailedAuthResult("No credentials found")
        }

        if (response.credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            Log.w(TAG, "Credential is not of type Google ID!")
            return FailedAuthResult("Credential is not of type Google ID!")
        }

        val googleIdTokenCredential =
            GoogleIdTokenCredential.createFrom(response.credential.data)

        val authResponse = apiClient.post<GoogleTokenRequestDto, TokenResponseDto>(
            ApiClientRoute.AUTH_GOOGLE,
            GoogleTokenRequestDto(googleToken = googleIdTokenCredential.idToken)
        )
        return handleAuthResponse(authResponse)
    }

    private suspend fun handleAuthResponse(response: ApiResponse<TokenResponseDto>): ApiBaseResponseResult {
        if (response.isSuccessful) {
            appDataRepository.updateCredentialsWithAPIResponse(response.body)
        }
        return response
    }

    suspend fun loadCurrentUserData(): MeParamsResponseDto? {
        val response = apiClient.get<MeParamsResponseDto>(ApiClientRoute.USER_ME_PARAMS)
        return response.body
    }

    companion object {
        private const val TAG = "AuthService"
    }
}

private class FailedAuthResult(
    private val message: String
) : ApiBaseResponseResult(Exception(message)) {
    override val errorMessage: String
        get() = message
}
