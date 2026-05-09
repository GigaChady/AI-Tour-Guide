package ai.tour.guide.network.ws

import ai.tour.guide.network.schema.response.NarrationResponseDto
import ai.tour.guide.network.schema.response.NarrationWordsResponseDto
import ai.tour.guide.network.schema.response.RoutePOIDto

sealed interface WSEvent {
    data object Connected : WSEvent
    data class Disconnected(val reason: String?) : WSEvent
}

sealed interface ServerEvent {
    data class SessionUpdated(val sessionId: String) : ServerEvent
    data class TourStarted(val sessionId: String) : ServerEvent
    data class NarrationTranscript(val data: NarrationResponseDto) : ServerEvent
    data class NarrationWords(val data: NarrationWordsResponseDto) : ServerEvent
    data class RoutePOIs(val data: RoutePOIDto) : ServerEvent
    data class EndOfStream(val sessionId: String) : ServerEvent
}
