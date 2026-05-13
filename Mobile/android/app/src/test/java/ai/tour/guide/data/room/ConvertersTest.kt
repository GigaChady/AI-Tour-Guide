package ai.tour.guide.data.room

import ai.tour.guide.network.schema.response.NarrationWordDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `fromNarrationWordDtoList returns null for null input`() {
        assertNull(converters.fromNarrationWordDtoList(null))
    }

    @Test
    fun `fromNarrationWordDtoList serializes list to JSON string`() {
        val list = listOf(NarrationWordDto(text = "hello", offsetMs = 100.0, durationMs = 200.0))
        val json = converters.fromNarrationWordDtoList(list)
        assertEquals(true, json?.contains("hello"))
    }

    @Test
    fun `toNarrationWordDtoList returns null for null input`() {
        assertNull(converters.toNarrationWordDtoList(null))
    }

    @Test
    fun `toNarrationWordDtoList round-trips with fromNarrationWordDtoList`() {
        val original = listOf(
            NarrationWordDto(text = "hello", offsetMs = 100.0, durationMs = 200.0),
            NarrationWordDto(text = "world", offsetMs = 300.0, durationMs = 150.0)
        )
        val json = converters.fromNarrationWordDtoList(original)
        val decoded = converters.toNarrationWordDtoList(json)
        assertEquals(original, decoded)
    }
}
