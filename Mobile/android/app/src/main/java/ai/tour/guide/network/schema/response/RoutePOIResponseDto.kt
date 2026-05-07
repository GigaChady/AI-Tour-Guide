package ai.tour.guide.network.schema.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoutePOIDto(
    val type: String,
    @SerialName("narration_id")
    val narrationId: String? = null,
    val data: List<ReceivedRoutePOI>
)

@Serializable
data class ReceivedRoutePOI(
    val name: String,
    val photos: List<String>,
    val desc: String,
    val lat: Double,
    val lng: Double,
)