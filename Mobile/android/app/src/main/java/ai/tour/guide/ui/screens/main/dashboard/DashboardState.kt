package ai.tour.guide.ui.screens.main.dashboard

data class DashboardState(
    val location: String,
) {
    companion object {
        fun default() = DashboardState("")
    }
}
