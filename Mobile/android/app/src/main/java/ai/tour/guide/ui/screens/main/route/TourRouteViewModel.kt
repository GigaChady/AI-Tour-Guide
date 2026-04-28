package ai.tour.guide.ui.screens.main.route

import ai.tour.guide.data.shared.BaseViewModel
import ai.tour.guide.domain.route.RouteService
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class TourRouteViewModel(val routeService: RouteService) :
    BaseViewModel<TourRouteState>(TourRouteState.default()) {
    private suspend fun onTourStart() {
        routeService.onStart()
        routeService.setNarrationChangedCallback { text ->
            updateData {
                copy(text = text)
            }
        }
    }

    fun onDestroy() {
        routeService.onDestroy()
    }

    fun onStart() {
        viewModelScope.launch {
            onTourStart()
        }
    }
}
