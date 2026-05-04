package ai.tour.guide.network.schema.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BaseWSRequestDto(
    val type: WSRequestType
)

@Serializable
enum class WSRequestType {
    @SerialName("token")
    TOKEN
}