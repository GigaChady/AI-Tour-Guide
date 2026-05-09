package ai.tour.guide.network.ws

import ai.tour.guide.network.schema.response.AudioChunkReceivedResponseDto
import ai.tour.guide.network.schema.response.NarrationResponseDto
import ai.tour.guide.network.schema.response.NarrationWordsResponseDto
import ai.tour.guide.network.schema.response.RoutePOIDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.ByteBuffer
import java.util.UUID

class WebSocketListeners {
    private var connectedListener: (suspend (WSEvent.Connected) -> Unit)? = null
    private var disconnectedListener: (suspend (WSEvent.Disconnected) -> Unit)? = null
    private var sessionUpdatedListener: (suspend (ServerEvent.SessionUpdated) -> Unit)? = null
    private var tourStartedListener: (suspend (ServerEvent.TourStarted) -> Unit)? = null
    private var narrationTranscriptListener: (suspend (data: NarrationResponseDto) -> Unit)? =
        null
    private var narrationWordsListener: (suspend (data: NarrationWordsResponseDto) -> Unit)? =
        null
    private var audioChunkReceivedListener: (suspend (data: AudioChunkReceivedResponseDto) -> Unit)? =
        null

    private var narrationPOIsListener: (suspend (data: RoutePOIDto) -> Unit)? = null
    private var endOfStreamListener: (suspend (ServerEvent.EndOfStream) -> Unit)? = null

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

    fun onAudioChunkReceived(listener: suspend (data: AudioChunkReceivedResponseDto) -> Unit) {
        audioChunkReceivedListener = listener
    }

    fun onRoutePOIsReceived(listener: suspend (data: RoutePOIDto) -> Unit) {
        narrationPOIsListener = listener
    }

    fun onEndOfStream(listener: suspend (ServerEvent.EndOfStream) -> Unit) {
        endOfStreamListener = listener
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

            "pois" -> {
                handleServerEvent(
                    ServerEvent.RoutePOIs(
                        Json.decodeFromString<RoutePOIDto>(event)
                    )
                )
            }

            "end_of_stream" -> {
                handleServerEvent(
                    ServerEvent.EndOfStream(sessionID)
                )
            }
        }
    }

    suspend fun handleAudioChunkReceived(data: ByteArray) {
        // Server binary layout (big-endian):
        // [0..15]  — session UUID (16 bytes)
        // [16..19] — chunk ID (4 bytes, uint32 big-endian)
        // [20..]   — audio payload
        val headerLength = 16 + 4  // UUID + chunk ID

        if (data.size <= headerLength) {
            return
        }

        // ByteBuffer is big-endian by default, matching Python's struct.pack(">...")
        val buffer = ByteBuffer.wrap(data)

        // 1. Extract session UUID (Bytes 0–15)
        val mostSigBits = buffer.long
        val leastSigBits = buffer.long
        val sessionId = UUID(mostSigBits, leastSigBits).toString()

        // 2. Extract chunk ID (Bytes 16–19)
        val chunkId = buffer.int

        // 3. Extract audio payload (Bytes 20+)
        val audioBytes = data.copyOfRange(headerLength, data.size)

        val payload = AudioChunkReceivedResponseDto(
            chunkId = chunkId,
            narrationId = sessionId,
            audioData = audioBytes
        )

        audioChunkReceivedListener?.invoke(payload)
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

    private suspend fun handleServerEvent(event: ServerEvent.RoutePOIs) {
        narrationPOIsListener?.invoke(event.data)
    }

    private suspend fun handleServerEvent(event: ServerEvent.EndOfStream) {
        endOfStreamListener?.invoke(event)
    }

    fun clearListeners() {
        connectedListener = null
        disconnectedListener = null
        sessionUpdatedListener = null
        tourStartedListener = null
        narrationTranscriptListener = null
        narrationWordsListener = null
        endOfStreamListener = null
    }
}
