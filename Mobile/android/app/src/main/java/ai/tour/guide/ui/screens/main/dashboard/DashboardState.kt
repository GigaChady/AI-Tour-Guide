package ai.tour.guide.ui.screens.main.dashboard

data class DashboardState(
    val poiPhotos: List<String>,
    val poiName: String,
) {
    companion object {
        fun default() = DashboardState(emptyList(), "")
    }
}
