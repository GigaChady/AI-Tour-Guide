package ai.tour.guide.network.ws

import ai.tour.guide.config.AppConfig
import ai.tour.guide.network.schema.response.NarrationResponseDto
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.json.JSONObject
import org.koin.core.annotation.Factory
import java.util.concurrent.TimeUnit

@Factory
class WSClient {
    private val webSocketListeners = WebSocketListeners()
    private var job: Job? = null
    private var session: WebSocketSession? = null
    private var currentRoute: WSClientRoute? = null
    private val scope =
        CoroutineScope(Dispatchers.IO) + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
            Log.d(
                TAG,
                "Error: ${throwable.message}",
            )
        }

    val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                pingInterval(5_000L, TimeUnit.MILLISECONDS)
            }
        }
        install(WebSockets)
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
    }

    fun onConnected(listener: suspend (WSEvent.Connected) -> Unit) {
        webSocketListeners.onConnected(listener)
    }

    fun onDisconnected(listener: suspend (WSEvent.Disconnected) -> Unit) {
        webSocketListeners.onDisconnected(listener)
    }

    fun onSessionUpdated(listener: suspend (ServerEvent.SessionUpdated) -> Unit) {
        webSocketListeners.onSessionUpdated(listener)
    }

    fun onTourStarted(listener: suspend (ServerEvent.TourStarted) -> Unit) {
        webSocketListeners.onTourStarted(listener)
    }

    fun onNarrationTranscript(listener: suspend (data: NarrationResponseDto) -> Unit) {
        webSocketListeners.onNarrationTranscript(listener)
    }

    fun onAudioChunkReceived(listener: suspend (data: ByteArray) -> Unit) {
        webSocketListeners.onAudioChunkReceived(listener)
    }

    fun onDestroy() {
        webSocketListeners.clearListeners()
        scope.launch {
            stop()
        }
    }

    suspend fun connect(
        route: WSClientRoute,
    ) {
        currentRoute = route
        job?.cancel()
        try {
            Log.d(
                TAG,
                "Connecting to websocket at ${route.path}..."
            )

            session = httpClient.webSocketSession {
                url {
                    protocol = URLProtocol.WS
                    host = AppConfig.HTTPS_CLIENT_HOST
                    path(route.path)
                }
            }
            webSocketListeners.handleWSEvent(WSEvent.Connected)

            Log.i(
                TAG,
                "Connected to websocket at ${route.path}"
            )
            job = scope.launch {
                session!!.incoming
                    .receiveAsFlow()
                    .filterNotNull()
                    .collect { data ->
                        when (data) {
                            is Frame.Text -> {
                                val text = data.readText()
                                scope.launch {
                                    webSocketListeners.handleRawEvent(text)
                                }
                                Log.i(
                                    TAG,
                                    "Received message: $text"
                                )
                            }

                            is Frame.Binary -> {
                                val data = data.readBytes()
                                webSocketListeners.handleAudioChunkReceived(data)
                                Log.i(
                                    TAG,
                                    "Received binary audio data of length ${data.size}"
                                )
                            }

                            else -> {
                                Log.i(
                                    TAG,
                                    "Received unknown data of type ${data.frameType}"
                                )
                            }
                        }
                    }
            }
        } catch (e: Exception) {
            webSocketListeners.handleWSEvent(WSEvent.Disconnected(e.message))
            Log.d(
                TAG,
                "Error: ${e.message}",
            )
            wsReconnect()
        }
    }

    private suspend fun wsReconnect() {
        job?.cancel()

        Log.d(
            TAG,
            "Reconnecting to websocket in ${RECONNECT_DELAY}ms..."
        )

        stop()
        delay(RECONNECT_DELAY)
        currentRoute?.let {
            connect(it)
        }
    }

    suspend fun stop() {
        Log.d(
            TAG,
            "Closing websocket session..."
        )

        session?.close()
        session = null
    }

    suspend fun send(message: JSONObject) {
        Log.d(
            TAG,
            "Sending message: $message"
        )

        session?.send(Frame.Text(message.toString()))
    }

    companion object {
        private const val TAG = "WSClient"
        private const val RECONNECT_DELAY = 10_000L
    }
}

enum class WSClientRoute(val path: String) {
    ROUTE("/route/ws")
}
