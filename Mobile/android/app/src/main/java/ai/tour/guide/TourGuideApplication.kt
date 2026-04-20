package ai.tour.guide

import ai.tour.guide.di.AppModule
import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.startKoin

@KoinApplication(modules = [AppModule::class])
class TourGuideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin<TourGuideApplication> {
            androidLogger()
            androidContext(this@TourGuideApplication)
        }
    }
}
