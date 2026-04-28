package ai.tour.guide.ui.screens.main.dashboard

import ai.tour.guide.data.shared.BaseViewModel
import ai.tour.guide.domain.route.RouteService
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DashboardViewModel(val routeService: RouteService) :
    BaseViewModel<DashboardState>(DashboardState.default()) {
    private suspend fun onViewMounted() {
        routeService.onStart()
    }

    fun onDestroy() {
        routeService.onDestroy()
    }

    fun onStart() {
        viewModelScope.launch {
            onViewMounted()
        }
    }
}