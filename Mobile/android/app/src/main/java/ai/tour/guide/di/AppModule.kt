package ai.tour.guide.di

import ai.tour.guide.data.room.AppDatabase
import android.content.Context
import androidx.room.Room
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("ai.tour.guide")
class AppModule {
    @Single
    fun provideAppDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tour_guide_db"
        ).build()
    }
}
