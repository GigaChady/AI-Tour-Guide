package ai.tour.guide.data.room.dao

import ai.tour.guide.data.room.entity.RouteSession
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RouteSessionDao {
    @Insert
    suspend fun insert(session: RouteSession): Long

    @Update
    suspend fun update(session: RouteSession)

    @Delete
    suspend fun delete(session: RouteSession)

    @Query("SELECT * from sessions ORDER BY created_at DESC LIMIT 1")
    fun getLatestSession(): LiveData<RouteSession>

}