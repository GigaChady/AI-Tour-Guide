package ai.tour.guide.ui.screens.main.dashboard

import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.data.shared.BaseViewModel
import ai.tour.guide.domain.route.RouteService
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DashboardViewModel(
    private val routeService: RouteService,
    private val appDatabase: AppDatabase
) : BaseViewModel<DashboardState>(DashboardState.default()) {
    private val sessionId = routeService.currentSessionIdFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    private val latestStopIdFlow = sessionId.flatMapLatest { id ->
        appDatabase.routeStopDao().getLatestStopIdForServerSession(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val firstPoiFlow = latestStopIdFlow.flatMapLatest { stopId ->
        appDatabase.routePOIDao().getFirstPoiForStop(stopId)
    }

    init {
        viewModelScope.launch {
            firstPoiFlow.collect { poi ->
                updateData {
                    copy(
                        poiPhotos = poi?.photosList().orEmpty().map(::normalizeLocalhostUrl),
                        poiName = poi?.name.orEmpty()
                    )
                }
            }
        }
    }

    fun onDestroy() {
    }

    fun onStart() {

    }

    private fun normalizeLocalhostUrl(url: String): String {
        return url
    }
}
