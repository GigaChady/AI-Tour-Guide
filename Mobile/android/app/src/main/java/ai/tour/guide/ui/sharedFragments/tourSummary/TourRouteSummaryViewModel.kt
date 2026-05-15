package ai.tour.guide.ui.sharedFragments.tourSummary

import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.data.room.entity.RoutePositionHistory
import ai.tour.guide.data.room.entity.toDto
import ai.tour.guide.data.route.RouteSessionDto
import ai.tour.guide.data.route.RouteStopDto
import ai.tour.guide.data.shared.BaseViewModel
import ai.tour.guide.domain.route.RouteNarrationPlaybackService
import ai.tour.guide.domain.route.RoutePlaybackState
import android.location.Location
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class TourRouteSummaryViewModel(
    appDatabase: AppDatabase,
    private val routeAudioService: RouteNarrationPlaybackService
) : BaseViewModel<TourRouteSummaryState>(TourRouteSummaryState()) {

    private val sessionDao = appDatabase.routeSessionDao()
    private val historyDao = appDatabase.routePosHistoryDao()
    private val stopDao = appDatabase.routeStopDao()

    private var hasStarted = false

    fun onStart() {
        if (hasStarted) return
        hasStarted = true

        observeLatestSession()
    }

    private fun observeLatestSession() {
        viewModelScope.launch {
            sessionDao.getLatestSessionFlow()
                .map { it?.toDto() }
                .collectLatest { sessionDto ->
                    if (sessionDto == null) {
                        updateData { TourRouteSummaryState() }
                        return@collectLatest
                    }
                    observeSessionDetails(sessionDto)
                }
        }
    }

    private suspend fun observeSessionDetails(session: RouteSessionDto) {
        combine(
            historyDao.getHistoryForSession(session.sessionId),
            stopDao.getStopMarkersForSession(session.sessionId),
            stopDao.getLatestStopIdForServerSession(session.serverSessionId),
            routeAudioService.playbackStateFlow
        ) { history, markersData, latestStopId, playback ->

            buildSummaryState(session, history, markersData, latestStopId, playback)

        }.collectLatest { newState ->
            updateData { newState }
        }
    }

    private fun buildSummaryState(
        session: RouteSessionDto,
        history: List<RoutePositionHistory>,
        markersData: List<RouteStopDto>,
        latestStopIdFromDb: Int?,
        playback: RoutePlaybackState
    ): TourRouteSummaryState {
        val distanceKm = calculateTotalDistance(history)
        val durationMin = calculateDurationMin(session)
        val progress = calculatePlaybackProgress(playback)

        val finalActiveStopId = if (session.endedAt != null) null else latestStopIdFromDb

        return TourRouteSummaryState(
            durationText = "${durationMin} min",
            distanceText = "%.1f km".format(distanceKm),
            attractionsCountText = markersData.size.toString(),
            visitedPlaces = markersData,
            activeStopId = finalActiveStopId,
            activePlaybackProgress = progress
        )
    }

    private fun calculateDurationMin(session: RouteSessionDto): Int {
        val endTime = session.endedAt ?: System.currentTimeMillis()
        return ((endTime - session.createdAt) / 60000).toInt()
    }

    private fun calculatePlaybackProgress(playback: RoutePlaybackState): Float {
        if (playback.durationMs <= 0) return 0f
        return playback.positionMs.toFloat() / playback.durationMs.toFloat()
    }

    // TODO: Optimize
    // Avoid checking each user position every time calculateTotalDistance is called
    // Store user history on the server
    private fun calculateTotalDistance(history: List<RoutePositionHistory>): Double {
        if (history.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until history.size - 1) {
            val results = FloatArray(1)
            Location.distanceBetween( // Calculate distance in meters
                history[i].lat, history[i].lng,
                history[i+1].lat, history[i+1].lng,
                results
            )
            total += results[0]
        }
        return total / 1000.0
    }
}