package ai.tour.guide.network.schema.response

import ai.tour.guide.data.appSettings.AppSettingsDetailLevelType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppSettingsResponseDto(
    @SerialName("language") val language: String? = null,
    @SerialName("pitch") val pitch: Int? = null,
    @SerialName("speed") val speed: Int? = null,
    @SerialName("volume") val volume: Int? = null,
    @SerialName("detail_level") val detailLevel: AppSettingsDetailLevelType? = null,
    @SerialName("auto_play") val autoPlay: Boolean? = null,
    override val detail: String? = null
) : IAPIResponseDto