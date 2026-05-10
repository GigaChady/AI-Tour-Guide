package ai.tour.guide.data.room.dao

import ai.tour.guide.data.room.entity.RouteStop
import ai.tour.guide.network.schema.response.NarrationWordDto
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteStopDao {
    @Insert(onConflict = IGNORE)
    suspend fun insert(stop: RouteStop): Long

    @Update
    suspend fun update(stop: RouteStop)

    @Delete
    suspend fun delete(stop: RouteStop)

    @Query("SELECT * FROM stops WHERE narration_id = :serverNarrationId")
    suspend fun getByServerId(serverNarrationId: String): RouteStop?

    @Query("UPDATE stops SET narration_words_map = :wordsMap WHERE id = :stopId")
    suspend fun updateNarrationWordsMapForStop(stopId: Int?, wordsMap: List<NarrationWordDto>?)

    @Query("UPDATE stops SET narration_audio_file_path = :filePath WHERE :narrationId IS NOT NULL AND narration_id = :narrationId")
    suspend fun updateNarrationFilePathForNarrationId(narrationId: String?, filePath: String)

    @Query("SELECT id FROM stops WHERE narration_id = :serverNarrationId")
    suspend fun getStopIdByNarration(serverNarrationId: String): Long

    @Transaction
    suspend fun getOrCreateStop(sessionId: Int, serverNarrationId: String): Long {
        val stop = RouteStop(sessionId = sessionId, serverNarrationId = serverNarrationId)
        val id = insert(stop)

        return if (id == -1L) {
            getStopIdByNarration(serverNarrationId)
        } else {
            id
        }
    }

    @Transaction
    suspend fun upsert(stop: RouteStop): Long {
        val existingStop = getByServerId(stop.serverNarrationId ?: "")
        return if (existingStop != null) {
            val updatedStop = stop.copy(id = existingStop.id)
            update(updatedStop)
            existingStop.id.toLong()
        } else {
            insert(stop)
        }
    }

    @Query("SELECT stops.id FROM stops JOIN sessions ON stops.session_id = sessions.id WHERE server_session_id = :serverSessionId ORDER BY stops.created_at DESC LIMIT 1")
    fun getLatestStopIdForServerSession(serverSessionId: String?): Flow<Int?>

    @Query("SELECT * FROM stops WHERE id = :stopId")
    fun getStopById(stopId: Int?): Flow<RouteStop?>

    @Query("SELECT EXISTS(SELECT 1 FROM stops JOIN sessions ON stops.session_id = sessions.id WHERE sessions.server_session_id = :serverSessionId AND stops.narration_audio_file_path IS NOT NULL LIMIT 1)")
    fun narrationFilesExistsForCurrentSession(serverSessionId: String?): Flow<Boolean>

    @Query("SELECT COUNT(stops.id) FROM stops JOIN sessions ON stops.session_id = sessions.id WHERE server_session_id = :serverSessionId")
    fun getStopsCountForServerSession(serverSessionId: String?): Flow<Int?>

    @Query("SELECT COUNT(*) FROM stops WHERE session_id = (SELECT session_id FROM stops WHERE id = :stopId) AND id <= :stopId")
    fun getStopsCountUntilStopId(stopId: Int?): Flow<Int?>

    @Query("SELECT COUNT(*) FROM stops WHERE session_id = (SELECT session_id FROM stops WHERE id = :stopId) AND id <= :stopId")
    fun getStopIndexById(stopId: Int?): Flow<Int?>

    @Query("SELECT stops.id FROM stops JOIN sessions ON stops.session_id = sessions.id WHERE server_session_id = :serverSessionId ORDER BY stops.id DESC LIMIT 1 OFFSET :offset")
    fun getStopIdByOffset(serverSessionId: String?, offset: Int): Flow<Int?>

    @Query("SELECT id FROM stops WHERE session_id = (SELECT session_id FROM stops WHERE id = :stopId) AND id <= :stopId ORDER BY id DESC LIMIT 1 OFFSET :offset")
    fun getStopIdByOffsetFromStop(stopId: Int?, offset: Int): Flow<Int?>
}
