package ai.tour.guide.network.schema.response

import kotlinx.serialization.Serializable

@Serializable
data class EmptyAPIResponse(
    override val detail: String? = null,
) : IAPIResponseDto
