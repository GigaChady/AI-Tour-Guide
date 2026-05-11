package ai.tour.guide.data.room.dao

import ai.tour.guide.data.room.entity.RoutePOI
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutePOIDao {
    @Insert
    suspend fun insert(poi: RoutePOI): Long

    @Update
    suspend fun update(poi: RoutePOI)

    @Delete
    suspend fun delete(poi: RoutePOI)

    @Insert
    suspend fun insertAll(pois: List<RoutePOI>)

    @Query("SELECT * FROM pois ORDER BY created_at DESC, id DESC LIMIT 1")
    fun getLatestPoi(): Flow<RoutePOI?>

}
