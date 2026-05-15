package ai.tour.guide.data.room.dao

import ai.tour.guide.data.room.entity.RouteSession
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteSessionDao {
    @Insert
    suspend fun insert(session: RouteSession): Long

    @Update
    suspend fun update(session: RouteSession)

    @Delete
    suspend fun delete(session: RouteSession)

    @Query("SELECT * from sessions ORDER BY created_at DESC LIMIT 1")
    fun getLatestSessionFlow(): Flow<RouteSession?>

    @Query("SELECT * FROM sessions WHERE server_session_id = :serverId LIMIT 1")
    suspend fun getSessionByServerId(serverId: String): RouteSession?

    @Query("UPDATE sessions SET ended_at = :endTime WHERE id = :sessionId")
    suspend fun updateEndedAt(sessionId: Int, endTime: Long)
}