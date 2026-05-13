package ai.tour.guide.ui.screens.main.dashboard

import ai.tour.guide.data.room.AppDatabase
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DashboardViewModel(
    private val appDatabase: AppDatabase
) {
    val latestPoiFlow = appDatabase.routePOIDao().getPrimaryPoiForLatestStop()
        .map { poi ->
            DashboardState(
                poiPhotos = poi?.photosList().orEmpty(),
                poiName = poi?.name.orEmpty()
            )
        }
}
