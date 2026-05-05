package ai.tour.guide.network.schema.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NarrationResponseDto(
    val type: String,
    val transcript: List<NarrationTranscriptChunkDto>
)

@Serializable
data class NarrationTranscriptChunkDto(
    @SerialName("chunk_id")
    val chunkId: Int,
    val text: String,
)

@Serializable
data class NarrationWordsResponseDto(
    val type: String,
    @SerialName("chunk_id")
    val chunkId: Int,
    val words: List<NarrationWordDto>
)

@Serializable
data class NarrationWordDto(
    val text: String,
    @SerialName("offset_ms")
    val offsetMs: Double,
    @SerialName("duration_ms")
    val durationMs: Double
) {
}
