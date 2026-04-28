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