package ai.tour.guide.ui.sharedFragments.tourSummary

import ai.tour.guide.data.route.RouteStopDto


data class TourRouteSummaryState(
    val durationText: String = "00:00",
    val distanceText: String = "0.0 km",
    val attractionsCountText: String = "0",
    val visitedPlaces: List<RouteStopDto> = emptyList(),
    val activeStopId: Int? = null,
    val activePlaybackProgress: Float = 0f
)