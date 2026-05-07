package ai.tour.guide.data.room.dao

import ai.tour.guide.data.room.entity.RoutePOI
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update

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

}