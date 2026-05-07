package ai.tour.guide.network.schema.request

import ai.tour.guide.data.appSettings.AppSettingsDetailLevelType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SaveAppSettingsRequestDto(

    val language: String? = null,

    val pitch: Int? = null,

   val speed: Int? = null,

    @SerialName("detail_level")
    val detailLevel: AppSettingsDetailLevelType? = null,

    @SerialName("auto_play")
    val autoPlay: Boolean? = null
)