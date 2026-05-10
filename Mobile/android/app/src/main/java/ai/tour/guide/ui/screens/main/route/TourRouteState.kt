package ai.tour.guide.ui.screens.main.route

import ai.tour.guide.network.schema.response.NarrationWordDto
import androidx.compose.ui.text.AnnotatedString

data class TourRouteState(
    val text: String,
    val styledText: AnnotatedString,
    val currentWordStartOffset: Int?,
    val words: List<NarrationWordDto>,
    val currentStopId: Int? = null,
    val currentLatestStopId: Int? = null,
    val currentHistoryOffset: Int = 0,
    val currentStopIndex: Int? = null,
    val totalStops: Int? = null,
    val pendingNextChunkRequestAfterStopId: Int? = null,
    val isSuccess: Boolean = false,
) {
    companion object {
        fun default() = TourRouteState(
            text = "",
            styledText = AnnotatedString(""),
            currentWordStartOffset = null,
            words = emptyList(),
            currentStopId = null,
            currentLatestStopId = null,
            currentHistoryOffset = 0,
            currentStopIndex = null,
            totalStops = null,
            pendingNextChunkRequestAfterStopId = null,
            isSuccess = false,
        )
    }
}
