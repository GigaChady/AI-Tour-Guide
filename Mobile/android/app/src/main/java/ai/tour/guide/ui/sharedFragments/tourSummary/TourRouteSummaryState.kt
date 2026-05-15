package ai.tour.guide.ui.sharedFragments.tourSummary

import ai.tour.guide.data.route.RouteStopDto


data class TourRouteSummaryState(
    val durationText: String = "-",
    val distanceText: String = "-",
    val attractionsCountText: String = "-",
    val visitedPlaces: List<RouteStopDto> = emptyList(),
    val activeStopId: Int? = null,
    val activePlaybackProgress: Float = 0f
)