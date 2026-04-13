package ai.tour.guide.network.schema.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleTokenRequestDto(
    @SerialName("google_token")
    val googleToken: String
)
