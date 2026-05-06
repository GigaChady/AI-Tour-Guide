package ai.tour.guide.domain

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.annotation.Single

@Single
class AppEventBus {
    private val _events = MutableSharedFlow<AppEventBusEvent>(replay = 0)
    val eventsFlow = _events.asSharedFlow()

    suspend fun publish(event: AppEventBusEvent) {
        Log.i(TAG, "publishing event: $event with type: ${event::class.simpleName}")
        _events.emit(event)
    }

    private companion object {
        const val TAG = "AppEventBus"
    }
}

sealed class AppEventBusEvent {
    data class AudioChunkReceived(val data: ByteArray) : AppEventBusEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AudioChunkReceived

            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }

    data class AudioChunkNearlyFinished(val position: Long) : AppEventBusEvent()
    data class RouteSessionStarted(val sessionId: String) : AppEventBusEvent()
}