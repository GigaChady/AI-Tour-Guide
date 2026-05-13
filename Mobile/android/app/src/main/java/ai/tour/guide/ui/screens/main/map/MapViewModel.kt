package ai.tour.guide.ui.screens.main.map

import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.data.shared.BaseViewModel
import ai.tour.guide.domain.location.LocationService
import ai.tour.guide.domain.route.RouteService
import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MapViewModel(
    private val appDatabase: AppDatabase,
    private val routeService: RouteService,
    private val locationService: LocationService
) : BaseViewModel<MapState>(MapState()) {

    private val sessionDao = appDatabase.routeSessionDao()
    private val historyDao = appDatabase.routePosHistoryDao()
    private val stopDao = appDatabase.routeStopDao()

    private var hasStarted = false

    fun onStart() {
        val hasPermission = locationService.hasLocationPermission()
        updateData { copy(isLocationPermissionGranted = hasPermission) }

        if (hasPermission) {
            viewModelScope.launch {
                try {
                    @SuppressLint("MissingPermission")
                    locationService.getLastKnownLocation()?.let { location ->
                        updateData { copy(lastKnownLocation = LatLng(location.latitude, location.longitude)) }
                    }
                } catch (e: Exception) {
                    Log.e("MapViewModel", "Failed to get last known location: $e")
                }
            }
        }

        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch {
            routeService.currentSessionIdFlow.collectLatest { serverSessionId ->

                if (serverSessionId == null) {
                    updateData { copy(userPath = emptyList(), visitedStops = emptyList()) }
                } else {
                    val session = sessionDao.getSessionByServerId(serverSessionId)

                    if (session != null) {
                        combine( // Synchronize two independent flows: HistoryFlow and StopMarkersFlow to constantly update map
                            historyDao.getHistoryForSession(session.id),
                            stopDao.getStopMarkersForSession(session.id)
                        ) { history, markersData ->
                            val path = history.map { LatLng(it.lat, it.lng) }
                            val stops = markersData.mapNotNull { data ->
                                if (data.lat != null && data.lng != null) {
                                    VisitedStop(
                                        id = data.stopId.toString(),
                                        title = data.title,
                                        snippet = data.snippet,
                                        position = LatLng(data.lat, data.lng)
                                    )
                                } else null
                            }
                            path to stops // Create pair (path, stops)
                        }.collectLatest { (path, stops) ->
                            updateData { copy(userPath = path, visitedStops = stops) }
                        }
                    }
                }
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        updateData { copy(isLocationPermissionGranted = granted) }
    }
}