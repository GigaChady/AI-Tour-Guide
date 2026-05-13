package ai.tour.guide.domain.route

import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.data.room.entity.RoutePOI
import ai.tour.guide.data.room.entity.RouteSession
import ai.tour.guide.domain.AppEventBus
import ai.tour.guide.domain.AppEventBusEvent
import ai.tour.guide.domain.location.LocationService
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.schema.response.AudioChunkReceivedResponseDto
import ai.tour.guide.network.schema.response.NarrationResponseDto
import ai.tour.guide.network.schema.response.NarrationWordDto
import ai.tour.guide.network.schema.response.NarrationWordsResponseDto
import ai.tour.guide.network.schema.response.RoutePOIDto
import ai.tour.guide.network.ws.ServerEvent
import ai.tour.guide.network.ws.WSClient
import ai.tour.guide.network.ws.WSClientRoute
import ai.tour.guide.network.ws.WSEvent
import android.Manifest
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.json.JSONObject
import org.koin.core.annotation.Single

@Single
class RouteService(
    private val appDataRepository: AppDataRepository,
    private val apiClient: ApiClient,
    private val wsClient: WSClient,
    private val eventBus: AppEventBus,
    private val locationService: LocationService,
    private val appDatabase: AppDatabase,
    private val appEventBus: AppEventBus,
    private val routeAudioRepository: RouteAudioRepository,
) {
    private var routeSession: RouteSession? = null
    private var lastRouteStopRowId: Int? = null

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionIdFlow: StateFlow<String?> = _currentSessionId.asStateFlow()

    private suspend fun setupLocalTourSession(sessionId: String) {
        val session = RouteSession(
            serverSessionId = sessionId,
        )
        val sessionDbId = appDatabase.routeSessionDao().insert(session)
        this.routeSession = session.copy(id = sessionDbId.toInt())
        _currentSessionId.value = sessionId
        eventBus.publish(AppEventBusEvent.RouteSessionStarted(sessionId))
        routeAudioRepository.startSession(sessionId)
    }

    private suspend fun getLastStopId(serverNarrationId: String?): Int {
        val sessionId = this.routeSession?.id ?: throw Exception("No session id")
        val serverNarrationId =
            serverNarrationId ?: throw Exception("Trying to insert a null serverNarrationId")
        val lastRouteStop =
            appDatabase.routeStopDao().getOrCreateStop(sessionId, serverNarrationId).toInt()
        this.lastRouteStopRowId = lastRouteStop
        return lastRouteStop
    }

    private suspend fun wsSessionEstablished(event: ServerEvent.SessionUpdated) {
        Log.i(TAG, "ws session established: ${event.sessionId}")
        setupLocalTourSession(event.sessionId)
        val payload = JSONObject().apply {
            put("type", "start_tour")
            put("session_id", event.sessionId)
        }
        wsClient.send(payload)
    }

    private suspend fun wsWordsMapReceived(data: NarrationWordsResponseDto) {
        val lastRouteStop = getLastStopId(data.narrationId)
        val words = Json.decodeFromJsonElement<List<NarrationWordDto>>(data.words)
        appDatabase.routeStopDao()
            .updateNarrationWordsMapForStop(lastRouteStop, words)
    }

    private suspend fun wsAudioChunkReceived(data: AudioChunkReceivedResponseDto) {
        val chunkFile = routeAudioRepository.appendChunk(data.audioData) ?: return
        appDatabase.routeStopDao()
            .updateNarrationFilePathForNarrationId(data.narrationId, chunkFile.path)
        eventBus.publish(AppEventBusEvent.AudioChunkReceived(chunkFile))
    }

    private suspend fun wsRoutePOISReceived(data: RoutePOIDto) {
        val sessionId = this.routeSession?.id ?: return
        val serverNarrationId = data.narrationId ?: return
        val stopId = appDatabase.routeStopDao().getOrCreateStop(sessionId, serverNarrationId)

        data.data.firstOrNull()?.let { firstPoi ->
            appDatabase.routeStopDao().updateLocationTitleAndImage(
                stopId = stopId.toInt(),
                title = firstPoi.name,
                image = firstPoi.photos.firstOrNull()
            )
        }

        appDatabase.routePOIDao().insertAll(data.data.mapIndexed { index, poi ->
            RoutePOI.fromReceivedPoi(
                data = poi,
                sessionId = sessionId,
                stopId = stopId.toInt(),
                poiIndex = index
            )
        })
    }

    private suspend fun wsNarrationTranscriptReceived(data: NarrationResponseDto) {
        val text = data.transcript.firstOrNull()?.text.orEmpty()
        val sessionId = this.routeSession?.id ?: return
        val serverNarrationId = data.narrationId ?: return
        val stopId = appDatabase.routeStopDao().getOrCreateStop(sessionId, serverNarrationId)

        appDatabase.routeStopDao().updateNarrationStringForStop(stopId.toInt(), text)
    }

    private suspend fun wsEndOfStreamReceived(event: ServerEvent.EndOfStream) {
        Log.i(TAG, "End of stream received for session: ${event.sessionId}")
    }

    private suspend fun wsOnTimeout(event: ServerEvent.Timeout) {
        appEventBus.publish(AppEventBusEvent.RouteTimeout(event.reason))
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
        wsClient.onNarrationWords(::wsWordsMapReceived)
        wsClient.onAudioChunkReceived(::wsAudioChunkReceived)
        wsClient.onRoutePOIsReceived(::wsRoutePOISReceived)
        wsClient.onEndOfStream(::wsEndOfStreamReceived)
        wsClient.onTimeout(::wsOnTimeout)
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

    suspend fun sendLastKnownLocation() {
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

    private suspend fun sendLocation(location: Location) {
        val payload = JSONObject().apply {
            put("lat", location.latitude)
            put("lng", location.longitude)
            put("ai", true)
        }
        wsClient.send(payload)
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun onStart() {
        apiClient.fetchBearerTokenIfNeeded()
        if (locationService.hasLocationPermission()) {
            locationService.startTracking()
        }
        initWSClient()
        wsBeginSession()
    }

    suspend fun onDestroy() {
        wsClient.onDestroy()
        locationService.stopTracking()
        routeAudioRepository.clearSession()
        _currentSessionId.value = null
        routeSession = null
        lastRouteStopRowId = null
    }

    private companion object {
        private const val TAG = "RouteService"
    }
}
