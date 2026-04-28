package ai.tour.guide.network.schema.response

import kotlinx.serialization.Serializable

@Serializable
data class MeParamsResponseDto(
    val email: String,
    val name: String,
    override val detail: String? = null,
) : IAPIResponseDto