package ai.tour.guide.network.ws

import ai.tour.guide.network.schema.response.AudioChunkReceivedResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.util.UUID

class WebSocketListenersTest {

    private fun buildAudioPayload(
        mostSigBits: Long,
        leastSigBits: Long,
        chunkId: Int,
        audio: ByteArray
    ): ByteArray {
        val buf = ByteBuffer.allocate(20 + audio.size)
        buf.putLong(mostSigBits)
        buf.putLong(leastSigBits)
        buf.putInt(chunkId)
        buf.put(audio)
        return buf.array()
    }

    @Test
    fun `handleAudioChunkReceived parses sessionId chunkId and audioPayload correctly`() =
        runBlocking {
            val msb = 123456789L
            val lsb = 987654321L
            val expectedUuid = UUID(msb, lsb).toString()
            val audio = byteArrayOf(1, 2, 3)
            val payload = buildAudioPayload(msb, lsb, 42, audio)

            var received: AudioChunkReceivedResponseDto? = null
            val listeners = WebSocketListeners()
            listeners.onAudioChunkReceived { received = it }

            listeners.handleAudioChunkReceived(payload)

            assertEquals(expectedUuid, received?.narrationId)
            assertEquals(42, received?.chunkId)
            assertArrayEquals(audio, received?.audioData)
        }

    @Test
    fun `handleAudioChunkReceived does not invoke listener when data is exactly 20 bytes`() =
        runBlocking {
            var called = false
            val listeners = WebSocketListeners()
            listeners.onAudioChunkReceived { called = true }

            listeners.handleAudioChunkReceived(ByteArray(20))

            assertEquals(false, called)
        }

    @Test
    fun `handleAudioChunkReceived does not crash when no listener is registered`() = runBlocking {
        val listeners = WebSocketListeners()
        val payload = buildAudioPayload(1L, 2L, 1, byteArrayOf(9))
        listeners.handleAudioChunkReceived(payload)
        // no assertion needed — must not throw
    }

    @Test
    fun `handleRawEvent dispatches session_start to sessionUpdatedListener`() = runBlocking {
        var sessionId: String? = null
        val listeners = WebSocketListeners()
        listeners.onSessionUpdated { sessionId = it.sessionId }

        listeners.handleRawEvent("""{"type":"session_start","session_id":"abc"}""")

        assertEquals("abc", sessionId)
    }

    @Test
    fun `handleRawEvent dispatches tour_start to tourStartedListener`() = runBlocking {
        var sessionId: String? = null
        val listeners = WebSocketListeners()
        listeners.onTourStarted { sessionId = it.sessionId }

        listeners.handleRawEvent("""{"type":"tour_start","session_id":"xyz"}""")

        assertEquals("xyz", sessionId)
    }

    @Test
    fun `handleRawEvent dispatches end_of_stream to endOfStreamListener`() = runBlocking {
        var sessionId: String? = null
        val listeners = WebSocketListeners()
        listeners.onEndOfStream { sessionId = it.sessionId }

        listeners.handleRawEvent("""{"type":"end_of_stream","session_id":"s99"}""")

        assertEquals("s99", sessionId)
    }

    @Test
    fun `handleRawEvent dispatches timeout detail to timeoutListener`() = runBlocking {
        var timeoutReceived = false
        val listeners = WebSocketListeners()
        listeners.onTimeout { timeoutReceived = true }

        listeners.handleRawEvent("""{"type":"other","detail":"timeout","session_id":"s1"}""")

        assertEquals(true, timeoutReceived)
    }

    @Test
    fun `clearListeners removes all listeners and subsequent call does not crash`() = runBlocking {
        var called = false
        val listeners = WebSocketListeners()
        listeners.onAudioChunkReceived { called = true }

        listeners.clearListeners()

        val payload = buildAudioPayload(1L, 2L, 1, byteArrayOf(9))
        listeners.handleAudioChunkReceived(payload)

        assertEquals(false, called)
    }
}
