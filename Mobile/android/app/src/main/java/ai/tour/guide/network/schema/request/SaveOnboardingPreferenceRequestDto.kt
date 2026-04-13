package ai.tour.guide.network.schema.request

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SaveOnboardingPreferenceRequestDto(
    @SerialName("question_key")
    val questionKey: String? = null,

    @SerialName("answer_key")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val answerKey: String? = null,

    @SerialName("answer_keys")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val answerKeys: List<String>? = null
)