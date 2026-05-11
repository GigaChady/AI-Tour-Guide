package ai.tour.guide.ui.screens.main.dashboard

import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.data.shared.BaseViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DashboardViewModel(
    private val appDatabase: AppDatabase
) : BaseViewModel<DashboardState>(DashboardState.default()) {
    private val latestPoiFlow = appDatabase.routePOIDao().getLatestPoi()
    private var latestPoiJob: Job? = null

    fun onDestroy() {
        latestPoiJob?.cancel()
        latestPoiJob = null
    }

    fun onStart() {
        if (latestPoiJob?.isActive == true) {
            return
        }

        latestPoiJob = viewModelScope.launch {
            latestPoiFlow.collect { poi ->
                updateData {
                    copy(
                        poiPhotos = poi?.photosList().orEmpty(),
                        poiName = poi?.name.orEmpty()
                    )
                }
            }
        }
    }
}
