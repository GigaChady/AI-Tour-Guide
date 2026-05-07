package ai.tour.guide.data.room.entity

import ai.tour.guide.network.schema.response.ReceivedRoutePOI
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.json.Json

@Entity(
    tableName = "pois",
    foreignKeys = [
        ForeignKey(
            entity = RouteStop::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("stop_id"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RouteSession::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("session_id"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )],
    indices = [
        Index(value = ["stop_id"]),
        Index(value = ["session_id"]),
    ]
)
data class RoutePOI(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Int? = null,
    @ColumnInfo(name = "stop_id")
    val stopId: Int? = null,
    val name: String,
    val photos: String,
    val desc: String,
    val lat: Double,
    val lng: Double,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromReceivedPoi(data: ReceivedRoutePOI, sessionId: Int?, stopId: Int?): RoutePOI =
            RoutePOI(
                sessionId = sessionId,
                stopId = stopId,
                name = data.name,
                photos = Json.encodeToString(data.photos),
                desc = data.desc,
                lat = data.lat,
                lng = data.lng
            )
    }
}
