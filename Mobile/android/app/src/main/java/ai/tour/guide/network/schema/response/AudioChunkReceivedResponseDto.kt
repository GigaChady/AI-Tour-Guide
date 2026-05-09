package ai.tour.guide.network.schema.response

data class AudioChunkReceivedResponseDto(
    val chunkId: Int? = null,
    val narrationId: String? = null,
    val audioData: ByteArray = byteArrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioChunkReceivedResponseDto

        if (chunkId != other.chunkId) return false
        if (narrationId != other.narrationId) return false
        if (!audioData.contentEquals(other.audioData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = chunkId ?: 0
        result = 31 * result + (narrationId?.hashCode() ?: 0)
        result = 31 * result + audioData.contentHashCode()
        return result
    }
}
