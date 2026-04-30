package ai.tour.guide.domain.route

import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.ws.ServerEvent
import ai.tour.guide.network.ws.WSClient
import ai.tour.guide.network.ws.WSClientRoute
import ai.tour.guide.network.ws.WSEvent
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val routeAudioRepository: RouteAudioRepository
) {
    private var sessionID: String = ""
    private var player: ExoPlayer? = null
    private var progressJob: Job? = null
    private var autoPlayEnabled: Boolean = true
    private val _isPlaying = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _hasPlayableChunks = MutableStateFlow(false)
    val hasPlayableChunksFlow: StateFlow<Boolean> = _hasPlayableChunks.asStateFlow()
    private val _playbackState = MutableStateFlow(RoutePlaybackState())
    val playbackStateFlow: StateFlow<RoutePlaybackState> = _playbackState.asStateFlow()

    private suspend fun wsSessionEstablished(event: ServerEvent.SessionUpdated) {
        Log.i(TAG, "ws session established: ${event.sessionId}")
        this.sessionID = event.sessionId
        autoPlayEnabled = true
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
        val payload1 = JSONObject().apply {
            put("lat", 1)
            put("lng", 1)
        }
        wsClient.send(payload1)
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

            if (currentPlayer.mediaItemCount == 0) {
                currentPlayer.setMediaItem(mediaItem)
                currentPlayer.prepare()
                if (autoPlayEnabled) {
                    currentPlayer.play()
                    _isPlaying.value = true
                } else {
                    _isPlaying.value = false
                }
                return@withContext
            }

            currentPlayer.addMediaItem(mediaItem)
            _hasPlayableChunks.value = true
            publishPlaybackState()
            if (autoPlayEnabled && !currentPlayer.isPlaying) {
                currentPlayer.play()
            }
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

    fun setNarrationChangedCallback(callback: (String) -> Unit) {
        wsClient.onNarrationTranscript { (_, data) ->
            callback(data[0].text)
        }
    }

    suspend fun onStart() {
        apiClient.fetchBearerTokenIfNeeded()
        initWSClient()
        wsBeginSession()
    }

    suspend fun onDestroy() {
        routeAudioRepository.clearSession()
        withContext(Dispatchers.Main.immediate) {
            progressJob?.cancel()
            progressJob = null
            player?.release()
            player = null
            autoPlayEnabled = true
            _isPlaying.value = false
            _hasPlayableChunks.value = false
            _playbackState.value = RoutePlaybackState()
        }
        wsClient.onDestroy()
    }

    companion object {
        private const val TAG = "RouteService"
    }
}
