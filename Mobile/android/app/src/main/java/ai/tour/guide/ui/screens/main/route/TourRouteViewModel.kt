package ai.tour.guide.ui.screens.main.route

import ai.tour.guide.data.shared.BaseViewModel
import ai.tour.guide.domain.route.RouteService
import ai.tour.guide.network.schema.response.NarrationWordDto
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
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
        viewModelScope.launch {
            combine(
                routeService.narrationTextFlow,
                routeService.narrationChunkIdFlow,
                routeService.narrationWordsFlow,
                routeService.playbackStateFlow
            ) { text, chunkId, words, playbackState ->
                val narrationText = buildNarrationText(
                    text = text,
                    words = words,
                    playbackPositionMs = playbackState.positionMs,
                    playbackDurationMs = playbackState.durationMs
                )
                TourRouteState(
                    text = text,
                    styledText = narrationText.text,
                    currentWordStartOffset = narrationText.currentWordStartOffset,
                    narrationChunkId = chunkId,
                    words = words
                )
            }.collect { routeState ->
                updateData {
                    routeState
                }
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

    private fun buildNarrationText(
        text: String,
        words: List<NarrationWordDto>,
        playbackPositionMs: Long,
        playbackDurationMs: Long
    ): NarrationTextPresentation {
        var currentWordStartOffset: Int? = null
        var searchStartIndex = 0
        val styledText = buildAnnotatedString {
            append(text)

            words.forEach { word ->
                if (word.text.isEmpty()) {
                    return@forEach
                }

                val wordStartOffset = text.indexOf(
                    string = word.text,
                    startIndex = searchStartIndex,
                    ignoreCase = false
                )
                if (wordStartOffset == -1) {
                    return@forEach
                }

                val wordEndOffset = wordStartOffset + word.text.length
                if (shouldHighlightWord(word, playbackPositionMs, playbackDurationMs)) {
                    currentWordStartOffset = wordStartOffset
                    addStyle(
                        style = SpanStyle(fontWeight = FontWeight.Bold),
                        start = wordStartOffset,
                        end = wordEndOffset
                    )
                }

                searchStartIndex = wordEndOffset
            }
        }

        return NarrationTextPresentation(
            text = styledText,
            currentWordStartOffset = currentWordStartOffset
        )
    }

    private fun shouldHighlightWord(
        word: NarrationWordDto,
        playbackPositionMs: Long,
        playbackDurationMs: Long
    ): Boolean {
        if (playbackDurationMs <= 0L) {
            return false
        }

        return playbackPositionMs.coerceAtMost(playbackDurationMs).toDouble() >= word.offsetMs
    }

    private data class NarrationTextPresentation(
        val text: AnnotatedString,
        val currentWordStartOffset: Int?
    )

}
