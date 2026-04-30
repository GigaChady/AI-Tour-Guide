package ai.tour.guide.ui.screens.main.route

import ai.tour.guide.data.shared.BaseViewModel
import ai.tour.guide.domain.route.RouteService
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class TourRouteViewModel(val routeService: RouteService) :
    BaseViewModel<TourRouteState>(TourRouteState.default()) {
    val isPlayingFlow: StateFlow<Boolean> = routeService.isPlayingFlow
    val playbackStateFlow = routeService.playbackStateFlow
    val hasPlayableChunksFlow: StateFlow<Boolean> = routeService.hasPlayableChunksFlow

    private suspend fun onTourStart() {
        routeService.setNarrationChangedCallback { text ->
            updateData {
                copy(text = text)
            }
        }
        routeService.onStart()
    }

    fun onDestroy() {
        viewModelScope.launch {
            routeService.onDestroy()
        }
    }

    fun onPlayClicked() {
        viewModelScope.launch {
            routeService.playNarration()
        }
    }

    fun onPauseClicked() {
        viewModelScope.launch {
            routeService.pauseNarration()
        }
    }

    fun onSkipPreviousClicked() {
        viewModelScope.launch {
            routeService.skipPreviousNarration()
        }
    }

    fun onSkipNextClicked() {
        viewModelScope.launch {
            routeService.skipNextNarration()
        }
    }

    fun onScrubTo(progressFraction: Float) {
        viewModelScope.launch {
            val playbackState = playbackStateFlow.value
            val duration = playbackState.durationMs
            if (duration <= 0L) {
                return@launch
            }
            val targetPosition = (duration * progressFraction.coerceIn(0f, 1f)).toLong()
            routeService.seekTo(targetPosition)
        }
    }

    fun onStart() {
        viewModelScope.launch {
            onTourStart()
        }
    }
}
