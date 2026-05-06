package ai.tour.guide.ui.screens.main.dashboard

import ai.tour.guide.data.shared.BaseViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DashboardViewModel :
    BaseViewModel<DashboardState>(DashboardState.default()) {
    private fun onViewMounted() {

    }

    fun onDestroy() {
    }

    fun onStart() {

    }
}
