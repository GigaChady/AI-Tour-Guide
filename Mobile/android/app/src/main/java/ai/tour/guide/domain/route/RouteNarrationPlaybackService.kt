package ai.tour.guide.domain.route

import ai.tour.guide.domain.AppEventBus
import ai.tour.guide.domain.AppEventBusEvent
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Singleton
import java.io.File

data class RoutePlaybackState(
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isEnded: Boolean = false
)

@Singleton
class RouteNarrationPlaybackService(
    private val context: Context,
    private val routeAudioRepository: RouteAudioRepository,
    private val eventBus: AppEventBus
) {
    private var player: ExoPlayer? = null
    private var progressJob: Job? = null
    private var hasBroadcastLocationNearCurrentNarrationEnd: Boolean = false
    private var autoPlayEnabled: Boolean = true
    private val _isPlaying = MutableStateFlow(false)
    private val _playbackState = MutableStateFlow(RoutePlaybackState())

    val isPlayingFlow: StateFlow<Boolean> = _isPlaying.asStateFlow()
    val playbackStateFlow: StateFlow<RoutePlaybackState> = _playbackState.asStateFlow()

    private suspend fun ensurePlayer() {
        withContext(Dispatchers.Main.immediate) {
            if (player != null) {
                return@withContext
            }
            player = ExoPlayer.Builder(context).build()
            player?.addListener(playerListener)
            startProgressPolling()
        }
    }

    private fun startProgressPolling() {
        if (progressJob?.isActive == true) {
            return
        }
        progressJob = CoroutineScope(Dispatchers.Main.immediate).launch {
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
            isPlaying = currentPlayer.isPlaying,
            isEnded = currentPlayer.playbackState == Player.STATE_ENDED
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

        if (positionMs in minPositionForNearEndBroadcast..<durationMs) {
            hasBroadcastLocationNearCurrentNarrationEnd = true
            CoroutineScope(Dispatchers.IO).launch {
                eventBus.publish(AppEventBusEvent.AudioChunkNearlyFinished(positionMs))
            }
        }
    }

    suspend fun playAudioFile(filePath: String) {
        withContext(Dispatchers.Main.immediate) {
            ensurePlayer()
            val currentPlayer = player ?: return@withContext

            currentPlayer.stop()
            currentPlayer.clearMediaItems()
            currentPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(filePath))))
            currentPlayer.prepare()

            hasBroadcastLocationNearCurrentNarrationEnd = false
            autoPlayEnabled = true
            currentPlayer.play()
            _isPlaying.value = true
            publishPlaybackState()
        }
    }

    suspend fun playNarration() {
        withContext(Dispatchers.Main.immediate) {
            ensurePlayer()
            autoPlayEnabled = true
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

    suspend fun onDestroy() {
        routeAudioRepository.clearSession()
        hasBroadcastLocationNearCurrentNarrationEnd = false
        withContext(Dispatchers.Main.immediate) {
            progressJob?.cancel()
            progressJob = null
            player?.release()
            player = null
            autoPlayEnabled = true
            _isPlaying.value = false
            _playbackState.value = RoutePlaybackState()
        }
    }

    fun onStart() {

    }

    val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            publishPlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            publishPlaybackState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                hasBroadcastLocationNearCurrentNarrationEnd = false
            }
            publishPlaybackState()
        }
    }

    private companion object {
        private const val NARRATION_END_LOCATION_BROADCAST_THRESHOLD_MS = 5_000L
        private const val SHORT_NARRATION_END_BROADCAST_FRACTION = 0.8
    }
}
