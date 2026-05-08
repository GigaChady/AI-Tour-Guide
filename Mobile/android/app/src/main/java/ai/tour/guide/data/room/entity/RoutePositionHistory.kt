package ai.tour.guide.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "position_history",
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
    ]
)
data class RoutePositionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Int? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
