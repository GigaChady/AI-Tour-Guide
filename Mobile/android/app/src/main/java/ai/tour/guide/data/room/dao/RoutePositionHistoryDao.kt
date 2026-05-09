package ai.tour.guide.data.room.dao

import ai.tour.guide.data.room.entity.RoutePositionHistory
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface RoutePositionHistoryDao {
    @Insert
    suspend fun insert(pos: RoutePositionHistory): Long

    @Update
    suspend fun update(pos: RoutePositionHistory)

    @Delete
    suspend fun delete(pos: RoutePositionHistory)

    @Query("SELECT id FROM sessions ORDER BY id DESC LIMIT 1")
    suspend fun getLatestSessionId(): Int?

    @Transaction
    suspend fun insertForLastSession(pos: RoutePositionHistory) {
        val latestSessionId = getLatestSessionId()
        val posToInsert = if (latestSessionId != null) {
            pos.copy(sessionId = latestSessionId)
        } else {
            pos
        }
        insert(posToInsert)
    }

}