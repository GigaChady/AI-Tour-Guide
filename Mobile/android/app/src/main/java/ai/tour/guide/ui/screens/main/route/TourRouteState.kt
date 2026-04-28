package ai.tour.guide.ui.screens.main.route

data class TourRouteState(
    val text: String
) {
    companion object {
        fun default() = TourRouteState("")
    }
}