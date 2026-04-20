package ai.tour.guide.network.schema.response

import ai.tour.guide.data.onboardingPreferences.OnboardingPreferencesDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class OnboardingPreferencesResponseDto(
    val items: List<OnboardingPreferencesDto> = emptyList(),
    override val detail: String? = null,
    @SerialName("selected_answers")
    var selectedAnswers: Map<String, JsonElement> = emptyMap()
) : IAPIResponseDto {
    fun getSelectedAnswer(key: String): String? =
        selectedAnswers[key]
            ?.takeIf { it is JsonPrimitive }
            ?.jsonPrimitive
            ?.contentOrNull

    fun getSelectedAnswers(key: String): List<String> =
        when (val value = selectedAnswers[key]) {
            is JsonArray -> value.mapNotNull { it.jsonPrimitive.contentOrNull }
            is JsonPrimitive -> value.contentOrNull?.let(::listOf).orEmpty()
            else -> emptyList()
        }
}
