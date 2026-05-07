package ai.tour.guide.data.room

import ai.tour.guide.data.room.dao.RoutePOIDao
import ai.tour.guide.data.room.dao.RouteSessionDao
import ai.tour.guide.data.room.dao.RouteStopDao
import ai.tour.guide.data.room.entity.RoutePOI
import ai.tour.guide.data.room.entity.RouteSession
import ai.tour.guide.data.room.entity.RouteStop
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [RouteSession::class, RouteStop::class, RoutePOI::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeSessionDao(): RouteSessionDao
    abstract fun routeStopDao(): RouteStopDao
    abstract fun routePOIDao(): RoutePOIDao
}