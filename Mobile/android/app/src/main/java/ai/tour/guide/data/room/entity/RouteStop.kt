package ai.tour.guide.data.room.entity

import ai.tour.guide.network.schema.response.NarrationWordDto
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stops",
    foreignKeys = [
        ForeignKey(
            entity = RouteSession::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("session_id"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["narration_id"], unique = true)
    ]
)
data class RouteStop(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Int,
    @ColumnInfo(name = "narration_id")
    val serverNarrationId: String? = null,
    @ColumnInfo(name = "location_title")
    val locationTitle: String? = null,
    @ColumnInfo(name = "location_image")
    val locationImage: String? = null,
    @ColumnInfo(name = "narration_string")
    val narrationString: String? = null,
    @ColumnInfo(name = "narration_words_map")
    val narrationWordsMap: List<NarrationWordDto>? = null,
    @ColumnInfo(name = "narration_audio_file_path")
    val narrationAudioFilePath: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
