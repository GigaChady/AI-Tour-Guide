package ai.tour.guide.ui.screens.main.route

import ai.tour.guide.data.shared.BaseViewModel
import ai.tour.guide.domain.route.RouteService
import ai.tour.guide.network.schema.response.NarrationWordDto
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class TourRouteViewModel(val routeService: RouteService) :
    BaseViewModel<TourRouteState>(TourRouteState.default()) {
    private var pendingNarrationText: String = ""
    private var pendingNarrationChunkId: Int? = null
    private var pendingNarrationWords: List<NarrationWordDto> = emptyList()
    private val currentNarrationText = MutableStateFlow("")
    private val currentNarrationChunkId = MutableStateFlow<Int?>(null)
    private val currentNarrationWords = MutableStateFlow<List<NarrationWordDto>>(emptyList())

    val isPlayingFlow: StateFlow<Boolean> = routeService.isPlayingFlow
    val playbackStateFlow = routeService.playbackStateFlow
    val hasPlayableChunksFlow: StateFlow<Boolean> = routeService.hasPlayableChunksFlow
    val currentNarrationTextFlow: StateFlow<String> = currentNarrationText.asStateFlow()

    private suspend fun onTourStart() {
        viewModelScope.launch {
            combine(
                currentNarrationText,
                currentNarrationChunkId,
                currentNarrationWords,
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

        viewModelScope.launch {
            combine(
                routeService.incomingNarrationTextFlow,
                routeService.incomingNarrationChunkIdFlow,
                routeService.incomingNarrationWordsFlow
            ) { text, chunkId, words ->
                Triple(text, chunkId, words)
            }.collect { (text, chunkId, words) ->
                pendingNarrationText = text
                pendingNarrationChunkId = chunkId
                pendingNarrationWords = words

                if (
                    currentNarrationText.value.isEmpty() &&
                    pendingNarrationText.isNotEmpty() &&
                    pendingNarrationWords.isNotEmpty()
                ) {
                    promotePendingNarration()
                }
            }
        }

        viewModelScope.launch {
            routeService.narrationPromotionTickFlow.collect {
                promotePendingNarration()
            }
        }

        routeService.onStart()
    }

    fun onDestroy() {
        viewModelScope.launch {
            resetNarrationState()
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

    private fun promotePendingNarration() {
        currentNarrationText.value = pendingNarrationText
        currentNarrationChunkId.value = pendingNarrationChunkId
        currentNarrationWords.value = pendingNarrationWords
    }

    private fun resetNarrationState() {
        pendingNarrationText = ""
        pendingNarrationChunkId = null
        pendingNarrationWords = emptyList()
        currentNarrationText.value = ""
        currentNarrationChunkId.value = null
        currentNarrationWords.value = emptyList()
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
