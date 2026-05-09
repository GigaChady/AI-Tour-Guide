package ai.tour.guide.data.room

import ai.tour.guide.network.schema.response.NarrationWordDto
import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromNarrationWordDtoList(value: List<NarrationWordDto>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toNarrationWordDtoList(value: String?): List<NarrationWordDto>? {
        return value?.let { Json.decodeFromString(it) }
    }
}
