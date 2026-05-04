package ai.tour.guide.data.appData

import ai.tour.guide.network.schema.response.TokenResponseDto
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

val Context.PersistedAppDataStore: DataStore<PersistedAppData> by dataStore(
    fileName = "app_data.json",
    serializer = PersistedAppDataSerializer
)

@Single
class AppDataRepository(private val context: Context) {
    private val _bearerToken = MutableStateFlow<String?>(null)
    val bearerTokenFlow = _bearerToken.asStateFlow()

    val onboardingCompletedFlow: Flow<Boolean> =
        context.PersistedAppDataStore.data.map { preferences ->
            preferences.onboardingCompleted
        }

    suspend fun updateCredentialsWithAPIResponse(response: TokenResponseDto?) {
        if (response == null) return
        _bearerToken.value = response.accessToken
        updateRefreshToken(response.refreshToken)
    }

    suspend fun updateRefreshToken(token: String?) {
        context.PersistedAppDataStore.updateData {
            it.copy(refreshToken = token)
        }
    }

    suspend fun updateOnboardingCompleted(completed: Boolean) {
        context.PersistedAppDataStore.updateData {
            it.copy(onboardingCompleted = completed)
        }
    }

    suspend fun updateBearerToken(authResponse: TokenResponseDto?) {
        if (authResponse == null) {
            return
        }
        _bearerToken.value = authResponse.accessToken
        updateRefreshToken(authResponse.refreshToken)
    }

    suspend fun shouldRefreshBearerToken(): Boolean {
        val tokenEmpty = _bearerToken.value == null
        val preferencesSnapshot = context.PersistedAppDataStore.data.first()
        return tokenEmpty && preferencesSnapshot.onboardingCompleted
    }

    suspend fun getRefreshToken(): String {
        val preferencesSnapshot = context.PersistedAppDataStore.data.first()
        val refreshToken = preferencesSnapshot.refreshToken
            ?: throw Exception("Trying to refresh bearer token with a null refresh")
        return refreshToken
    }
}
