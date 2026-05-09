package ai.tour.guide.ui.screens.main.route

import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.data.shared.BaseViewModel
import ai.tour.guide.domain.AppEventBus
import ai.tour.guide.domain.route.RouteNarrationPlaybackService
import ai.tour.guide.domain.route.RouteService
import ai.tour.guide.network.schema.response.NarrationWordDto
import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class TourRouteViewModel(
    val routeAudioService: RouteNarrationPlaybackService,
    val routeService: RouteService,
    val appEventBus: AppEventBus,
    val appDatabase: AppDatabase
) :
    BaseViewModel<TourRouteState>(TourRouteState.default()) {
    val isPlayingFlow: StateFlow<Boolean> = routeAudioService.isPlayingFlow
    val playbackStateFlow = routeAudioService.playbackStateFlow

    private val sessionId: StateFlow<String?> = routeService.currentSessionIdFlow
    private val currentStopId = MutableStateFlow<Int?>(null)
    private var lastPlayedStopId: Int? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _stopFlow = currentStopId.flatMapLatest { stopId ->
        Log.i(TAG, "Getting stop $stopId")
        appDatabase.routeStopDao().getStopById(stopId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _latestStopId = sessionId.flatMapLatest { id ->
        appDatabase.routeStopDao().getLatestStopIdForServerSession(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val playerEnabledFlow = sessionId.flatMapLatest { id ->
        appDatabase.routeStopDao().narrationFilesExistsForCurrentSession(id)
    }

    private val eventBusScope = CoroutineScope(Dispatchers.Default)

    private fun startStopStateListeners() {
        viewModelScope.launch {
            combine(_latestStopId, playbackStateFlow) { latestStopId, playback ->
                Pair(latestStopId, playback)
            }.collect { (latestStopId, playback) ->
                val current = currentStopId.value
                if (current == null) {
                    currentStopId.value = latestStopId
                } else if (latestStopId != null && current != latestStopId) {
                    if (playback.isEnded) {
                        Log.i(TAG, "Changing current stop to $latestStopId")
                        currentStopId.value = latestStopId
                    }
                }
            }
        }

        viewModelScope.launch {
            combine(_stopFlow, playbackStateFlow) { stop, playback ->
                Pair(stop, playback)
            }.collect { (stop, playback) ->
                val text = stop?.narrationString ?: ""
                val words = stop?.narrationWordsMap ?: emptyList()

                val narrationText = buildNarrationText(
                    text = text,
                    words = words,
                    playbackPositionMs = playback.positionMs,
                    playbackDurationMs = playback.durationMs
                )
                updateData {
                    TourRouteState(
                        text = text,
                        styledText = narrationText.text,
                        currentWordStartOffset = narrationText.currentWordStartOffset,
                        words = words
                    )
                }
            }
        }

        viewModelScope.launch {
            _stopFlow.collect { stop ->
                val filePath = stop?.narrationAudioFilePath
                if (stop != null && filePath != null && stop.id != lastPlayedStopId) {
                    lastPlayedStopId = stop.id
                    routeAudioService.playAudioFile(filePath)
                }
            }
        }
    }

    private suspend fun initEventListeners() {
        appEventBus.eventsFlow.collect { event ->
            when (event) {
                else -> {}
            }
        }
    }

    private suspend fun onTourStart() {
        try {
            routeService.onStart()
            routeAudioService.onStart()
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing location permission, cant start $TAG", e)
        }

    }

    fun onDestroy() {
        viewModelScope.launch {
            routeAudioService.onDestroy()
            routeService.onDestroy()
        }
        currentStopId.value = null
        lastPlayedStopId = null
        updateData { TourRouteState.default() }
    }

    fun onPlayClicked() {
        viewModelScope.launch {
            routeAudioService.playNarration()
        }
    }

    fun onPauseClicked() {
        viewModelScope.launch {
            routeAudioService.pauseNarration()
        }
    }


    fun onStart() {
        startStopStateListeners()
        eventBusScope.launch {
            initEventListeners()
        }
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

    private companion object {
        const val TAG = "TourRouteViewModel"
    }
}

data class NarrationTextPresentation(
    val text: AnnotatedString,
    val currentWordStartOffset: Int?
)
