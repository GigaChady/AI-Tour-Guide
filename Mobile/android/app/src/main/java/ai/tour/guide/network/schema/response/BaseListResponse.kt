package ai.tour.guide.network.schema.response

import kotlinx.serialization.Serializable

@Serializable
data class BaseListResponse<T>(
    val items: List<T> = emptyList(),
    override val detail: String? = null
) : IAPIResponseDto
