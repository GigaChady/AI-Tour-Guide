package ai.tour.guide.ui.screens.main.route

import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.data.shared.BaseViewModel
import ai.tour.guide.domain.AppEventBus
import ai.tour.guide.domain.AppEventBusEvent
import ai.tour.guide.domain.route.RouteNarrationPlaybackService
import ai.tour.guide.domain.route.RouteService
import ai.tour.guide.network.schema.response.NarrationWordDto
import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    private var lastPlayedStopId: Int? = null
    private var stopStateListenersJob: Job? = null
    private var eventListenersJob: Job? = null

    private val currentStopId = viewStateFlow.map { it.data.currentStopId }.distinctUntilChanged()
    private val currentLatestStopId =
        viewStateFlow.map { it.data.currentLatestStopId }.distinctUntilChanged()
    private val currentHistoryOffset =
        viewStateFlow.map { it.data.currentHistoryOffset }.distinctUntilChanged()

    private var hasStarted = false

    @OptIn(ExperimentalCoroutinesApi::class)
    val stopsCount = currentLatestStopId.flatMapLatest { stopId ->
        appDatabase.routeStopDao().getStopsCountUntilStopId(stopId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedStopId =
        combine(currentLatestStopId, currentHistoryOffset) { stopId, offset ->
            Pair(stopId, offset)
        }.flatMapLatest { (stopId, offset) ->
            appDatabase.routeStopDao().getStopIdByOffsetFromStop(stopId, offset)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentStopIndex = currentStopId.flatMapLatest { stopId ->
        appDatabase.routeStopDao().getStopIndexById(stopId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentStopFlow = currentStopId.flatMapLatest { stopId ->
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

    private fun startStopStateListeners() {
        if (stopStateListenersJob?.isActive == true) {
            return
        }

        stopStateListenersJob = viewModelScope.launch {
            launch { initializeCurrentLatestStop() }
            launch { advanceStopWhenPlaybackEnds() }
            launch { advanceStopWhenRequestedChunkArrives() }
            launch { syncCurrentStopWithSelectedStop() }
            launch { updateRoutePresentationState() }
            launch { playCurrentStopAudio() }
        }
    }

    private suspend fun initializeCurrentLatestStop() {
        _latestStopId.collect { latestStopId ->
            if (viewStateFlow.value.data.currentLatestStopId == null) {
                updateData {
                    copy(currentLatestStopId = latestStopId)
                }
            }
        }
    }

    private suspend fun advanceStopWhenPlaybackEnds() {
        combine(
            _latestStopId,
            playbackStateFlow.map { it.isEnded }.distinctUntilChanged()
        ) { latestStopId, isPlaybackEnded ->
            Pair(latestStopId, isPlaybackEnded)
        }.collect { (latestStopId, isPlaybackEnded) ->
            if (!isPlaybackEnded) {
                return@collect
            }

            val data = viewStateFlow.value.data
            val offset = data.currentHistoryOffset
            if (offset > 0) {
                updateData {
                    copy(currentHistoryOffset = offset - 1)
                }
                return@collect
            }

            val current = data.currentLatestStopId
            if (current == null) {
                updateData {
                    copy(currentLatestStopId = latestStopId)
                }
            } else if (latestStopId != null && current != latestStopId) {
                Log.i(TAG, "Changing current stop to $latestStopId")
                updateData {
                    copy(currentLatestStopId = latestStopId)
                }
            }
        }
    }

    private suspend fun advanceStopWhenRequestedChunkArrives() {
        combine(
            _latestStopId,
            viewStateFlow.map { it.data.pendingNextChunkRequestAfterStopId }.distinctUntilChanged()
        ) { latestStopId, requestedAfterStopId ->
            Pair(latestStopId, requestedAfterStopId)
        }.collect { (latestStopId, requestedAfterStopId) ->
            if (latestStopId == null || requestedAfterStopId == null || latestStopId == requestedAfterStopId) {
                return@collect
            }

            updateData {
                copy(
                    currentHistoryOffset = 0,
                    currentLatestStopId = latestStopId,
                    pendingNextChunkRequestAfterStopId = null
                )
            }
        }
    }

    private suspend fun syncCurrentStopWithSelectedStop() {
        selectedStopId.collect { stopId ->
            if (stopId != null) {
                updateData {
                    copy(currentStopId = stopId)
                }
            } else {
                updateData {
                    copy(
                        currentHistoryOffset = 0,
                        currentStopId = currentLatestStopId
                    )
                }
            }
        }
    }

    private suspend fun updateRoutePresentationState() {
        combine(
            currentStopFlow,
            playbackStateFlow,
            currentStopIndex,
            stopsCount
        ) { stop, playback, index, total ->
            Triple(stop, playback, Pair(index, total))
        }.collect { (stop, playback, counts) ->
            val (index, total) = counts
            val text = stop?.narrationString ?: ""
            val words = stop?.narrationWordsMap ?: emptyList()

            val narrationText = buildNarrationText(
                text = text,
                words = words,
                playbackPositionMs = playback.positionMs,
                playbackDurationMs = playback.durationMs
            )
            updateData {
                copy(
                    text = text,
                    styledText = narrationText.text,
                    currentWordStartOffset = narrationText.currentWordStartOffset,
                    words = words,
                    currentStopIndex = index,
                    totalStops = total
                )
            }
        }
    }

    private suspend fun playCurrentStopAudio() {
        currentStopFlow.collect { stop ->
            val filePath = stop?.narrationAudioFilePath
            if (stop != null && filePath != null && stop.id != lastPlayedStopId) {
                lastPlayedStopId = stop.id
                routeAudioService.playAudioFile(filePath)
            }
        }
    }

    private suspend fun initEventListeners() {
        appEventBus.eventsFlow.collect { event ->
            when (event) {
                is AppEventBusEvent.AudioChunkNearlyFinished -> {
                    val offset = viewStateFlow.value.data.currentHistoryOffset
                    Log.i(
                        TAG,
                        "current offset is $offset, ${if (offset == 0) "requesting" else "skipping"} sending location"
                    )
                    if (offset == 0) {
                        routeService.sendLastKnownLocation()
                    }
                }

                is AppEventBusEvent.RouteTimeout -> {
                    Log.i(TAG, "Route timeout: ${event.reason}")
                    updateData {
                        copy(
                            isSuccess = true,
                        )
                    }
                }

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
        stopStateListenersJob?.cancel()
        stopStateListenersJob = null
        eventListenersJob?.cancel()
        eventListenersJob = null
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
        if (hasStarted) return
        hasStarted = true

        startStopStateListeners()
        if (eventListenersJob?.isActive != true) {
            eventListenersJob = viewModelScope.launch {
                initEventListeners()
            }
        }
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

    fun onNextClicked() {
        val data = viewStateFlow.value.data
        val offset = data.currentHistoryOffset
        if (offset > 0) {
            updateData {
                copy(currentHistoryOffset = offset - 1)
            }
            return
        }

        updateData {
            copy(pendingNextChunkRequestAfterStopId = data.currentLatestStopId)
        }
        viewModelScope.launch {
            routeService.sendLastKnownLocation()
        }
    }

    fun onPrevClicked() {
        val totalStops = viewStateFlow.value.data.totalStops ?: return
        val maxOffset = (totalStops - 1).coerceAtLeast(0)
        updateData {
            copy(currentHistoryOffset = (currentHistoryOffset + 1).coerceAtMost(maxOffset))
        }
    }

    fun endTour() {
        onDestroy()
    }

    override fun onCleared() {
        super.onCleared()
        onDestroy()
    }

    private companion object {
        const val TAG = "TourRouteViewModel"
    }
}

data class NarrationTextPresentation(
    val text: AnnotatedString,
    val currentWordStartOffset: Int?
)
