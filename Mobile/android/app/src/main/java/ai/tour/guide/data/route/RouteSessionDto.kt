package ai.tour.guide.data.route


data class RouteSessionDto(
    val sessionId: Int,
    val serverSessionId: String?,
    val createdAt: Long,
    val endedAt: Long?
)