package ai.tour.guide.network.schema.response

import kotlinx.serialization.Serializable

@Serializable
open class BaseAPIResponse(open val detail: String?)