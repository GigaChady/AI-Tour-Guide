package ai.tour.guide.data.room.entity

import ai.tour.guide.data.route.RouteSessionDto
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class RouteSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "server_session_id")
    val serverSessionId: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "ended_at")
    val endedAt: Long? = null
)

// TODO: Add repository
fun RouteSession.toDto(): RouteSessionDto = RouteSessionDto(
    sessionId = this.id,
    serverSessionId = this.serverSessionId,
    createdAt = this.createdAt,
    endedAt = this.endedAt
)
