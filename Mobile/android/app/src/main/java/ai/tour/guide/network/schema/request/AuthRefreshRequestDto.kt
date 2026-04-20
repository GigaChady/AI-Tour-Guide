package ai.tour.guide.network.schema.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRefreshRequestDto(
    @SerialName("refresh_token")
    val refreshToken: String
)