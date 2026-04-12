package ai.tour.guide.data.appData

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

val Context.PersistedAppDataStore: DataStore<PersistedAppData> by dataStore(
    fileName = "app_data.json",
    serializer = PersistedAppDataSerializer
)

@Single
class AppDataRepository(private val context: Context) {
    val onboardingCompletedFlow: Flow<Boolean> =
        context.PersistedAppDataStore.data.map { preferences ->
            preferences.onboardingCompleted
        }

    suspend fun updateOnboardingCompleted(completed: Boolean) {
        context.PersistedAppDataStore.updateData {
            it.copy(onboardingCompleted = completed)
        }
    }

    suspend fun updateRefreshToken(refreshToken: String?) {
        context.PersistedAppDataStore.updateData {
            it.copy(refreshToken = refreshToken)
        }
    }
}
