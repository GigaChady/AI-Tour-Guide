package ai.tour.guide.data

import ai.tour.guide.data.models.PersistedAppData
import ai.tour.guide.data.serializers.PersistedAppDataSerializer
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore

val Context.PersistedAppDataStore: DataStore<PersistedAppData> by dataStore(
    fileName = "app_data.json",
    serializer = PersistedAppDataSerializer
)