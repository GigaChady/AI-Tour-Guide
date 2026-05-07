package ai.tour.guide.network.schema.response

import ai.tour.guide.data.appSettings.AppSettingsAppThemeType
import ai.tour.guide.data.appSettings.AppSettingsDetailLevelType
import ai.tour.guide.data.appSettings.AppSettingsDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppSettingsResponseDto(

    val language: String? = null,

    val pitch: Int? = null,

    val speed: Int? = null,

    @SerialName("detail_level")
    val detailLevel: AppSettingsDetailLevelType? = null,

    @SerialName("auto_play")
    val autoPlay: Boolean? = null,

    override val detail: String? = null
) : IAPIResponseDto

// AppSettingsResponseDto needs conversion to AppSettingsDto before further usage
fun AppSettingsResponseDto.toDto(currentTheme: AppSettingsAppThemeType? = null): AppSettingsDto {
    return AppSettingsDto(
        appTheme = currentTheme, // Added from local storage
        language = this.language,
        pitch = this.pitch,
        speed = this.speed,
        detailLevel = this.detailLevel,
        autoPlay = this.autoPlay
    )
}