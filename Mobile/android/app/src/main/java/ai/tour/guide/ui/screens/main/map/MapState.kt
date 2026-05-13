package ai.tour.guide.ui.screens.main.map

import com.google.android.gms.maps.model.LatLng

data class MapState(
    val lastKnownLocation: LatLng? = null,
    val userPath: List<LatLng> = emptyList(),
    val visitedStops: List<VisitedStop> = emptyList(),
    val isLoading: Boolean = false,
    val isLocationPermissionGranted: Boolean = false
)

data class VisitedStop(
    val id: String,
    val title: String?,
    val snippet: String?,
    val position: LatLng
)