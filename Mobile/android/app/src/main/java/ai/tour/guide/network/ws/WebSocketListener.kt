package ai.tour.guide.network.ws

import ai.tour.guide.network.schema.response.NarrationResponseDto
import ai.tour.guide.network.schema.response.NarrationWordsResponseDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class WebSocketListeners {
    private var connectedListener: (suspend (WSEvent.Connected) -> Unit)? = null
    private var disconnectedListener: (suspend (WSEvent.Disconnected) -> Unit)? = null
    private var sessionUpdatedListener: (suspend (ServerEvent.SessionUpdated) -> Unit)? = null
    private var tourStartedListener: (suspend (ServerEvent.TourStarted) -> Unit)? = null
    private var narrationTranscriptListener: (suspend (data: NarrationResponseDto) -> Unit)? =
        null
    private var narrationWordsListener: (suspend (data: NarrationWordsResponseDto) -> Unit)? =
        null
    private var audioChunkReceivedListener: (suspend (data: ByteArray) -> Unit)? = null

    fun onConnected(listener: suspend (WSEvent.Connected) -> Unit) {
        connectedListener = listener
    }

    fun onDisconnected(listener: suspend (WSEvent.Disconnected) -> Unit) {
        disconnectedListener = listener
    }

    fun onSessionUpdated(listener: suspend (ServerEvent.SessionUpdated) -> Unit) {
        sessionUpdatedListener = listener
    }

    fun onTourStarted(listener: suspend (ServerEvent.TourStarted) -> Unit) {
        tourStartedListener = listener
    }

    fun onNarrationTranscript(listener: suspend (data: NarrationResponseDto) -> Unit) {
        narrationTranscriptListener = listener
    }

    fun onNarrationWords(listener: suspend (data: NarrationWordsResponseDto) -> Unit) {
        narrationWordsListener = listener
    }

    fun onAudioChunkReceived(listener: suspend (data: ByteArray) -> Unit) {
        audioChunkReceivedListener = listener
    }

    suspend fun handleWSEvent(event: WSEvent.Connected) {
        val handler = connectedListener ?: throw Exception("No handler for event $event")
        handler.invoke(event)
    }

    suspend fun handleWSEvent(event: WSEvent.Disconnected) {
        val handler = disconnectedListener ?: throw Exception("No handler for event $event")
        handler.invoke(event)
    }

    suspend fun handleRawEvent(event: String) {
        val data = Json.parseToJsonElement(event).jsonObject
        val eventType = data["type"]?.jsonPrimitive?.content
        val sessionID = data["session_id"]?.jsonPrimitive?.content ?: ""
        when (eventType) {
            "session_start" -> {
                handleServerEvent(
                    ServerEvent.SessionUpdated(sessionID)
                )
            }

            "tour_start" -> {
                handleServerEvent(
                    ServerEvent.TourStarted(sessionID)
                )
            }

            "narration_transcript" -> {
                handleServerEvent(
                    ServerEvent.NarrationTranscript(
                        Json.decodeFromString<NarrationResponseDto>(event)
                    ),
                )
            }

            "narration_words" -> {
                handleServerEvent(
                    ServerEvent.NarrationWords(
                        Json.decodeFromString<NarrationWordsResponseDto>(event)
                    ),
                )
            }
        }
    }

    suspend fun handleAudioChunkReceived(data: ByteArray) {
        audioChunkReceivedListener?.invoke(data)
    }

    private suspend fun handleServerEvent(event: ServerEvent.SessionUpdated) {
        sessionUpdatedListener?.invoke(event)
    }

    private suspend fun handleServerEvent(event: ServerEvent.TourStarted) {
        tourStartedListener?.invoke(event)
    }

    private suspend fun handleServerEvent(event: ServerEvent.NarrationTranscript) {
        narrationTranscriptListener?.invoke(event.data)
    }

    private suspend fun handleServerEvent(event: ServerEvent.NarrationWords) {
        narrationWordsListener?.invoke(event.data)
    }

    fun clearListeners() {
        connectedListener = null
        disconnectedListener = null
        sessionUpdatedListener = null
        tourStartedListener = null
        narrationTranscriptListener = null
        narrationWordsListener = null
    }
}
