package ai.tour.guide.domain

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.annotation.Single
import java.io.File

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
    data class AudioChunkReceived(val file: File) : AppEventBusEvent()
    data class AudioChunkNearlyFinished(val position: Long) : AppEventBusEvent()
    data class RouteSessionStarted(val sessionId: String) : AppEventBusEvent()
    data class RouteTimeout(val reason: String?) : AppEventBusEvent()
}