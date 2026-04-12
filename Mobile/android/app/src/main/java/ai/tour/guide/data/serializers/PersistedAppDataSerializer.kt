package ai.tour.guide.data.serializers

import ai.tour.guide.data.models.PersistedAppData
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object PersistedAppDataSerializer : Serializer<PersistedAppData> {
    override val defaultValue: PersistedAppData = PersistedAppData()

    override suspend fun readFrom(input: InputStream): PersistedAppData =
        try {
            Json.decodeFromString<PersistedAppData>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read PersistedAppData", serialization)
        }

    override suspend fun writeTo(t: PersistedAppData, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(t)
                    .encodeToByteArray()
            )
        }
    }
}