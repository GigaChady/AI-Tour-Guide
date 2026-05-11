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

    @Query(
        """
        SELECT pois.* FROM pois
        INNER JOIN stops ON pois.stop_id = stops.id
        ORDER BY stops.created_at DESC, stops.id DESC, pois.poi_index ASC, pois.id ASC
        LIMIT 1
        """
    )
    fun getPrimaryPoiForLatestStop(): Flow<RoutePOI?>

}
