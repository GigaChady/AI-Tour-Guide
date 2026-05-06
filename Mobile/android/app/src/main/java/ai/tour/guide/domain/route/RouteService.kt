package ai.tour.guide.domain.route

import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.data.room.entity.RouteSession
import ai.tour.guide.data.room.entity.RouteStop
import ai.tour.guide.domain.AppEventBus
import ai.tour.guide.domain.AppEventBusEvent
import ai.tour.guide.domain.location.LocationService
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.schema.response.NarrationResponseDto
import ai.tour.guide.network.schema.response.NarrationWordDto
import ai.tour.guide.network.ws.ServerEvent
import ai.tour.guide.network.ws.WSClient
import ai.tour.guide.network.ws.WSClientRoute
import ai.tour.guide.network.ws.WSEvent
import android.location.Location
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.core.annotation.Single

@Single
class RouteService(
    private val appDataRepository: AppDataRepository,
    private val apiClient: ApiClient,
    private val wsClient: WSClient,
    private val eventBus: AppEventBus,
    private val locationService: LocationService,
    private val appDatabase: AppDatabase
) {
    private var cachedLocation: Location? = null
    private var routeSession: RouteSession? = null
    private val _narrationText = MutableStateFlow("")
    val narrationTextFlow: StateFlow<String> = _narrationText.asStateFlow()
    private val _narrationWords = MutableStateFlow<List<NarrationWordDto>>(emptyList())
    val narrationWordsFlow: StateFlow<List<NarrationWordDto>> =
        _narrationWords.asStateFlow()
    private val _narrationChunkId = MutableStateFlow<Int?>(null)
    val narrationChunkIdFlow: StateFlow<Int?> = _narrationChunkId.asStateFlow()
    private val scope = CoroutineScope(Dispatchers.Default)

    private suspend fun wsSessionEstablished(event: ServerEvent.SessionUpdated) {
        Log.i(TAG, "ws session established: ${event.sessionId}")

        val session = RouteSession(
            serverSessionId = event.sessionId,
        )
        appDatabase.routeSessionDao().insert(session)
        this.routeSession = session
        eventBus.publish(AppEventBusEvent.RouteSessionStarted(event.sessionId))

        val payload = JSONObject().apply {
            put("type", "start_tour")
            put("session_id", event.sessionId)
        }
        wsClient.send(payload)
    }

    private suspend fun wsAudioChunkReceived(data: ByteArray) {
        eventBus.publish(AppEventBusEvent.AudioChunkReceived(data))
    }

    private suspend fun wsNarrationTranscriptReceived(data: NarrationResponseDto) {
        val text = data.transcript.firstOrNull()?.text.orEmpty()
        val stop = RouteStop(
            sessionId = this.routeSession?.id ?: return,
            narrationString = text
        )
        appDatabase.routeStopDao().insert(stop)
        _narrationText.value = text
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
        wsClient.onNarrationTranscript(::wsNarrationTranscriptReceived)
        wsClient.onNarrationWords { data ->
            _narrationChunkId.value = data.chunkId
            _narrationWords.value = data.words
        }
        wsClient.onAudioChunkReceived(::wsAudioChunkReceived)
        wsClient.connect(WSClientRoute.ROUTE)
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

    private suspend fun sendLocation(location: Location) {
        val payload = JSONObject().apply {
            put("lat", location.latitude)
            put("lng", location.longitude)
        }
        wsClient.send(payload)
    }

    suspend fun onStart() {
        apiClient.fetchBearerTokenIfNeeded()
        initWSClient()
        wsBeginSession()
        scope.launch {
            startEventBusListeners()
        }
    }

    fun onDestroy() {
        cachedLocation = null
        wsClient.onDestroy()
    }

    suspend fun startEventBusListeners() {
        eventBus.eventsFlow.collect { event ->
            when (event) {
                is AppEventBusEvent.AudioChunkNearlyFinished -> {
                    sendLastKnownLocation()
                }

                else -> {}
            }
        }
    }

    private companion object {
        private const val TAG = "RouteService"
    }
}