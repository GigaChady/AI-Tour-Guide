package ai.tour.guide.domain.route

import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.ws.ServerEvent
import ai.tour.guide.network.ws.WSClient
import ai.tour.guide.network.ws.WSClientRoute
import ai.tour.guide.network.ws.WSEvent
import android.util.Log
import org.json.JSONObject
import org.koin.core.annotation.Singleton
import kotlin.io.encoding.Base64

@Singleton
class RouteService(
    val appDataRepository: AppDataRepository,
    val apiClient: ApiClient,
    private val wsClient: WSClient
) {
    private var sessionID: String = ""

    private suspend fun wsSessionEstablished(event: ServerEvent.SessionUpdated) {
        Log.i(TAG, "ws session established: ${event.sessionId}")
        this.sessionID = event.sessionId
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
        val data = Base64.encode(data)
        Log.i(TAG, data)
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

    fun onDestroy() {
        wsClient.onDestroy()
    }

    companion object {
        private const val TAG = "RouteService"
    }
}
