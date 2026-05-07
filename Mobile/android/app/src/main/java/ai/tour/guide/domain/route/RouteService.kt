package ai.tour.guide.domain.route

import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.domain.location.LocationService
import ai.tour.guide.network.schema.response.NarrationWordDto
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.ws.ServerEvent
import ai.tour.guide.network.ws.WSClient
import ai.tour.guide.network.ws.WSClientRoute
import ai.tour.guide.network.ws.WSEvent
import android.content.Context
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.koin.core.annotation.Singleton

data class RoutePlaybackState(
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false
)

@Singleton
class RouteService(
    val appDataRepository: AppDataRepository,
    val apiClient: ApiClient,
    private val wsClient: WSClient,
    private val context: Context,
    private val routeAudioRepository: RouteAudioRepository,
    private val locationService: LocationService
) {
    private var sessionID: String = ""
    private var player: ExoPlayer? = null
    private var progressJob: Job? = null
    private var locationUpdatesJob: Job? = null
    private var cachedLocation: Location? = null
    private var isWaitingForForcedSkip: Boolean = false
    private var hasBroadcastLocationNearCurrentNarrationEnd: Boolean = false
    private var autoPlayEnabled: Boolean = true
    private val _isPlaying = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _hasPlayableChunks = MutableStateFlow(false)
    val hasPlayableChunksFlow: StateFlow<Boolean> = _hasPlayableChunks.asStateFlow()
    private val _playbackState = MutableStateFlow(RoutePlaybackState())
    val playbackStateFlow: StateFlow<RoutePlaybackState> = _playbackState.asStateFlow()
    private val _incomingNarrationText = MutableStateFlow("")
    val incomingNarrationTextFlow: StateFlow<String> = _incomingNarrationText.asStateFlow()
    private val _incomingNarrationWords = MutableStateFlow<List<NarrationWordDto>>(emptyList())
    val incomingNarrationWordsFlow: StateFlow<List<NarrationWordDto>> = _incomingNarrationWords.asStateFlow()
    private val _incomingNarrationChunkId = MutableStateFlow<Int?>(null)
    val incomingNarrationChunkIdFlow: StateFlow<Int?> = _incomingNarrationChunkId.asStateFlow()
    private val _narrationPromotionTick = MutableStateFlow(0L)
    val narrationPromotionTickFlow: StateFlow<Long> = _narrationPromotionTick.asStateFlow()

    private suspend fun wsSessionEstablished(event: ServerEvent.SessionUpdated) {
        Log.i(TAG, "ws session established: ${event.sessionId}")
        this.sessionID = event.sessionId
        autoPlayEnabled = true
        cachedLocation = null
        isWaitingForForcedSkip = false
        hasBroadcastLocationNearCurrentNarrationEnd = false
        clearIncomingNarrationState()
        routeAudioRepository.startSession(event.sessionId)
        val payload = JSONObject().apply {
            put("type", "start_tour")
            put("session_id", sessionID)
        }
        wsClient.send(payload)
    }

    private suspend fun wsBeginSession() {
        appDataRepository.bearerTokenFlow.value?.let { token ->
            val payload = JSONObject().apply {
                put("token", token)
            }
            wsClient.send(payload)
        }
    }

    private suspend fun wsRouteStarted(event: ServerEvent.TourStarted) {
        Log.i(TAG, "ws route started  ${event.sessionId}")
        sendLastKnownLocation()
        startLocationUpdatesBroadcast()
    }

    private suspend fun sendLastKnownLocation() {
        val location = try {
            locationService.getLastKnownLocation()
        } catch (exception: SecurityException) {
            Log.w(TAG, "Cannot send last known location: ${exception.message}")
            null
        }

        if (location == null) {
            Log.w(TAG, "Last known location is unavailable")
            return
        }

        cachedLocation = location
        sendLocation(location)
    }

    private fun startLocationUpdatesBroadcast() {
        if (locationUpdatesJob?.isActive == true) {
            return
        }

        locationUpdatesJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                locationService.observeLocationUpdates(emitLastKnownLocation = false)
                    .collectLatest { location ->
                        cachedLocation = location
                    }
            } catch (exception: SecurityException) {
                Log.w(TAG, "Cannot observe location updates: ${exception.message}")
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.w(TAG, "Location updates stopped: ${exception.message}")
            }
        }
    }

    private suspend fun sendCachedLocation() {
        val location = cachedLocation ?: try {
            locationService.getLastKnownLocation()
        } catch (exception: SecurityException) {
            Log.w(TAG, "Cannot send cached location: ${exception.message}")
            null
        }

        if (location == null) {
            Log.w(TAG, "Cached location is unavailable")
            return
        }

        cachedLocation = location
        sendLocation(location)
    }

    private suspend fun sendLocation(location: Location) {
        val payload = JSONObject().apply {
            put("lat", location.latitude)
            put("lng", location.longitude)
        }
        wsClient.send(payload)
    }

    private suspend fun initWSClient() {
        wsClient.onConnected { event: WSEvent.Connected ->
            Log.i(TAG, "ws connected: $event")
        }
        wsClient.onDisconnected { event: WSEvent.Disconnected ->
            Log.i(TAG, "ws disconnected: ${event.reason}")
        }
        wsClient.onSessionUpdated(::wsSessionEstablished)
        wsClient.onTourStarted(::wsRouteStarted)
        wsClient.onNarrationTranscript { data ->
            _incomingNarrationText.value = data.transcript.firstOrNull()?.text.orEmpty()
        }
        wsClient.onNarrationWords { data ->
            _incomingNarrationChunkId.value = data.chunkId
            _incomingNarrationWords.value = data.words
        }
        wsClient.onAudioChunkReceived(::wsAudioChunkReceived)
        wsClient.connect(WSClientRoute.ROUTE)
    }

    private suspend fun wsAudioChunkReceived(data: ByteArray) {
        val chunkFile = routeAudioRepository.appendChunk(data) ?: return
        _hasPlayableChunks.value = true
        enqueueChunk(chunkFile)
    }

    private suspend fun ensurePlayer() {
        withContext(Dispatchers.Main.immediate) {
            if (player != null) {
                return@withContext
            }
            player = ExoPlayer.Builder(context).build()
            player?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    publishPlaybackState()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    publishPlaybackState()
                    if (playbackState == Player.STATE_ENDED) {
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            sendCachedLocation()
                        }
                        requestNarrationPromotion()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        hasBroadcastLocationNearCurrentNarrationEnd = false
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            sendCachedLocation()
                        }
                        requestNarrationPromotion()
                    }
                    publishPlaybackState()
                }
            })
            startProgressPolling()
        }
    }

    private fun startProgressPolling() {
        if (progressJob?.isActive == true) {
            return
        }
        progressJob = kotlinx.coroutines.CoroutineScope(Dispatchers.Main.immediate).launch {
            while (isActive) {
                publishPlaybackState()
                delay(100L)
            }
        }
    }

    private fun publishPlaybackState() {
        val currentPlayer = player ?: run {
            _playbackState.value = RoutePlaybackState()
            return
        }

        val duration = when {
            currentPlayer.duration > 0L -> currentPlayer.duration
            else -> 0L
        }
        _playbackState.value = RoutePlaybackState(
            positionMs = currentPlayer.currentPosition.coerceAtLeast(0L),
            bufferedPositionMs = currentPlayer.bufferedPosition.coerceAtLeast(0L),
            durationMs = duration,
            isPlaying = currentPlayer.isPlaying
        )
        maybeBroadcastLocationNearNarrationEnd(currentPlayer.currentPosition, duration)
    }

    private fun maybeBroadcastLocationNearNarrationEnd(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L || hasBroadcastLocationNearCurrentNarrationEnd) {
            return
        }

        val minPositionForNearEndBroadcast = when {
            durationMs <= NARRATION_END_LOCATION_BROADCAST_THRESHOLD_MS ->
                (durationMs * SHORT_NARRATION_END_BROADCAST_FRACTION).toLong()
            else -> durationMs - NARRATION_END_LOCATION_BROADCAST_THRESHOLD_MS
        }

        if (positionMs >= minPositionForNearEndBroadcast && positionMs < durationMs) {
            hasBroadcastLocationNearCurrentNarrationEnd = true
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                sendCachedLocation()
            }
        }
    }

    private suspend fun preparePlayer() {
        withContext(Dispatchers.Main.immediate) {
            val currentPlayer = player ?: return@withContext
            val chunkFiles = routeAudioRepository.getChunkFiles()
            if (chunkFiles.isEmpty()) {
                return@withContext
            }

            currentPlayer.apply {
                stop()
                clearMediaItems()
                setMediaItems(chunkFiles.map { file ->
                    MediaItem.fromUri(Uri.fromFile(file))
                })
                prepare()
                playWhenReady = true
            }
            _isPlaying.value = true
        }
    }

    private suspend fun enqueueChunk(chunkFile: java.io.File) {
        withContext(Dispatchers.Main.immediate) {
            ensurePlayer()
            val currentPlayer = player ?: return@withContext
            val mediaItem = MediaItem.fromUri(Uri.fromFile(chunkFile))

            if (currentPlayer.mediaItemCount == 0 || currentPlayer.playbackState == Player.STATE_ENDED || isWaitingForForcedSkip) {
                isWaitingForForcedSkip = false
                requestNarrationPromotion()
                currentPlayer.stop()
                currentPlayer.clearMediaItems()
                currentPlayer.setMediaItem(mediaItem)
                currentPlayer.prepare()
                if (autoPlayEnabled) {
                    currentPlayer.play()
                }
                hasBroadcastLocationNearCurrentNarrationEnd = false
            } else {
                currentPlayer.addMediaItem(mediaItem)
                currentPlayer.prepare()
                currentPlayer.playWhenReady = autoPlayEnabled
            }

            autoPlayEnabled = true
            _isPlaying.value = true
            _hasPlayableChunks.value = true
            publishPlaybackState()
        }
    }

    suspend fun playNarration() {
        withContext(Dispatchers.Main.immediate) {
            ensurePlayer()
            autoPlayEnabled = true
            if (player?.mediaItemCount == 0) {
                preparePlayer()
            }
            if (player?.playbackState == Player.STATE_ENDED) {
                player?.seekToDefaultPosition(0)
            }
            if (player?.currentMediaItemIndex == -1) {
                player?.seekToDefaultPosition(0)
            }
            player?.play()
            _isPlaying.value = player?.isPlaying == true
        }
    }

    suspend fun pauseNarration() {
        withContext(Dispatchers.Main.immediate) {
            autoPlayEnabled = false
            player?.pause()
            _isPlaying.value = false
        }
    }

    suspend fun skipPreviousNarration() {
        withContext(Dispatchers.Main.immediate) {
            val currentPlayer = player ?: return@withContext
            currentPlayer.seekToPreviousMediaItem()
        }
    }

    suspend fun skipNextNarration() {
        sendCachedLocation()
        withContext(Dispatchers.Main.immediate) {
            val currentPlayer = player ?: return@withContext
            if (currentPlayer.hasNextMediaItem()) {
                requestNarrationPromotion()
                currentPlayer.seekToNextMediaItem()
            } else {
                isWaitingForForcedSkip = true
                currentPlayer.stop()
            }
        }
    }

    suspend fun seekTo(positionMs: Long) {
        withContext(Dispatchers.Main.immediate) {
            player?.seekTo(positionMs.coerceAtLeast(0L))
            publishPlaybackState()
        }
    }

    suspend fun onStart() {
        apiClient.fetchBearerTokenIfNeeded()
        initWSClient()
        wsBeginSession()
    }

    suspend fun onDestroy() {
        routeAudioRepository.clearSession()
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
        cachedLocation = null
        isWaitingForForcedSkip = false
        hasBroadcastLocationNearCurrentNarrationEnd = false
        withContext(Dispatchers.Main.immediate) {
            progressJob?.cancel()
            progressJob = null
            player?.release()
            player = null
            autoPlayEnabled = true
            _isPlaying.value = false
            _hasPlayableChunks.value = false
            _playbackState.value = RoutePlaybackState()
            clearIncomingNarrationState()
        }
        wsClient.onDestroy()
    }

    private fun requestNarrationPromotion() {
        _narrationPromotionTick.value += 1L
    }

    private fun clearIncomingNarrationState() {
        _incomingNarrationText.value = ""
        _incomingNarrationWords.value = emptyList()
        _incomingNarrationChunkId.value = null
        _narrationPromotionTick.value = 0L
    }

    companion object {
        private const val TAG = "RouteService"
        private const val NARRATION_END_LOCATION_BROADCAST_THRESHOLD_MS = 5_000L
        private const val SHORT_NARRATION_END_BROADCAST_FRACTION = 0.8
    }
}
