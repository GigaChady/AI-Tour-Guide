package ai.tour.guide.ui.sharedFragments.tourSummary

import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.data.room.entity.RoutePositionHistory
import ai.tour.guide.data.shared.BaseViewModel
import ai.tour.guide.domain.route.RouteNarrationPlaybackService
import ai.tour.guide.domain.route.RouteService
import android.location.Location
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.math.roundToInt

@KoinViewModel
class TourRouteSummaryViewModel(
    private val appDatabase: AppDatabase,
    private val routeService: RouteService,
    private val routeAudioService: RouteNarrationPlaybackService
) : BaseViewModel<TourRouteSummaryState>(TourRouteSummaryState()) {

    private val sessionDao = appDatabase.routeSessionDao()
    private val historyDao = appDatabase.routePosHistoryDao()
    private val stopDao = appDatabase.routeStopDao()

    private var hasStarted = false

    fun onStart() {
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch {
            sessionDao.getLatestSessionFlow().collectLatest { session ->
                if (session == null) {
                    updateData { TourRouteSummaryState() }
                    return@collectLatest
                }

                combine(
                    historyDao.getHistoryForSession(session.id),
                    stopDao.getStopMarkersForSession(session.id),
                    stopDao.getLatestStopIdForServerSession(session.serverSessionId),
                    routeAudioService.playbackStateFlow
                ) { history, markersData, latestStopId, playback ->

                    val distanceKm = calculateTotalDistance(history)
                    val durationMin = ((System.currentTimeMillis() - session.createdAt) / 60000).toInt()

                    val progress = if (playback.durationMs > 0) {
                        playback.positionMs.toFloat() / playback.durationMs.toFloat()
                    } else 0f

                    TourRouteSummaryState(
                        durationText = "${durationMin} min",
                        distanceText = "${(distanceKm * 10).roundToInt() / 10.0} km",
                        attractionsCountText = markersData.size.toString(),
                        visitedPlaces = markersData,
                        activeStopId = latestStopId,
                        activePlaybackProgress = progress
                    )
                }.collectLatest { newState ->
                    updateData { newState }
                }
            }
        }
    }

    private fun calculateTotalDistance(history: List<RoutePositionHistory>): Double {
        if (history.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until history.size - 1) {
            val results = FloatArray(1)
            Location.distanceBetween(
                history[i].lat, history[i].lng,
                history[i+1].lat, history[i+1].lng,
                results
            )
            total += results[0]
        }
        return total / 1000.0
    }
}