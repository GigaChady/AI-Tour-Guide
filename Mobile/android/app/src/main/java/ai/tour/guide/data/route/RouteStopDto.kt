package ai.tour.guide.data.route

data class RouteStopDto(
    val stopId: Int,
    val title: String?,
    val snippet: String?,
    val lat: Double?,
    val lng: Double?
)