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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
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

private data class SentLocation(
    val latitude: Double,
    val longitude: Double
)

@Singleton
@OptIn(FlowPreview::class)
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
    private var lastSentLocation: SentLocation? = null
    private var autoPlayEnabled: Boolean = true
    private val _isPlaying = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _hasPlayableChunks = MutableStateFlow(false)
    val hasPlayableChunksFlow: StateFlow<Boolean> = _hasPlayableChunks.asStateFlow()
    private val _playbackState = MutableStateFlow(RoutePlaybackState())
    val playbackStateFlow: StateFlow<RoutePlaybackState> = _playbackState.asStateFlow()
    private val _narrationText = MutableStateFlow("")
    val narrationTextFlow: StateFlow<String> = _narrationText.asStateFlow()
    private val _narrationWords = MutableStateFlow<List<NarrationWordDto>>(emptyList())
    val narrationWordsFlow: StateFlow<List<NarrationWordDto>> = _narrationWords.asStateFlow()
    private val _narrationChunkId = MutableStateFlow<Int?>(null)
    val narrationChunkIdFlow: StateFlow<Int?> = _narrationChunkId.asStateFlow()

    private suspend fun wsSessionEstablished(event: ServerEvent.SessionUpdated) {
        Log.i(TAG, "ws session established: ${event.sessionId}")
        this.sessionID = event.sessionId
        autoPlayEnabled = true
        lastSentLocation = null
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

        sendLocation(location)
    }

    private fun startLocationUpdatesBroadcast() {
        if (locationUpdatesJob?.isActive == true) {
            return
        }

        locationUpdatesJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                locationService.observeLocationUpdates(emitLastKnownLocation = false)
                    .debounce(LOCATION_UPDATE_DEBOUNCE_MS)
                    .collectLatest { location ->
                        sendLocation(location)
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

    private suspend fun sendLocation(location: Location) {
        val nextLocation = SentLocation(
            latitude = location.latitude,
            longitude = location.longitude
        )
        if (lastSentLocation == nextLocation) {
            Log.d(TAG, "Skipping duplicate location update")
            return
        }

        val payload = JSONObject().apply {
            put("lat", nextLocation.latitude)
            put("lng", nextLocation.longitude)
        }
        wsClient.send(payload)
        lastSentLocation = nextLocation
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
            _narrationText.value = data.transcript.firstOrNull()?.text.orEmpty()
        }
        wsClient.onNarrationWords { data ->
            _narrationChunkId.value = data.chunkId
            _narrationWords.value = data.words
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
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
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

            currentPlayer.stop()
            currentPlayer.clearMediaItems()
            currentPlayer.setMediaItem(mediaItem)
            currentPlayer.prepare()
            currentPlayer.play()
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
        withContext(Dispatchers.Main.immediate) {
            player?.seekToNextMediaItem()
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
        lastSentLocation = null
        withContext(Dispatchers.Main.immediate) {
            progressJob?.cancel()
            progressJob = null
            player?.release()
            player = null
            autoPlayEnabled = true
            _isPlaying.value = false
            _hasPlayableChunks.value = false
            _playbackState.value = RoutePlaybackState()
            _narrationText.value = ""
            _narrationWords.value = emptyList()
            _narrationChunkId.value = null
        }
        wsClient.onDestroy()
    }

    companion object {
        private const val TAG = "RouteService"
        private const val LOCATION_UPDATE_DEBOUNCE_MS = 2_000L
    }
}
