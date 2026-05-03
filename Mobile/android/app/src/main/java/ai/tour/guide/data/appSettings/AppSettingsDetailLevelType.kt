package ai.tour.guide.data.appSettings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AppSettingsDetailLevelType {
    @SerialName("low")
    LOW,

    @SerialName("medium")
    MEDIUM,

    @SerialName("high")
    HIGH
}