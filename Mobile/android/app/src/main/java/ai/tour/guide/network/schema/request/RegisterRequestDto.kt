package ai.tour.guide.network.schema.request

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String
)