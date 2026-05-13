package ai.tour.guide.network.schema.response

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingPreferencesResponseDtoTest {

    private fun dto(vararg pairs: Pair<String, kotlinx.serialization.json.JsonElement>) =
        OnboardingPreferencesResponseDto(selectedAnswers = mapOf(*pairs))

    @Test
    fun `getSelectedAnswer returns String for JsonPrimitive value`() {
        val result = dto("key" to JsonPrimitive("val")).getSelectedAnswer("key")
        assertEquals("val", result)
    }

    @Test
    fun `getSelectedAnswer returns null for missing key`() {
        val result = dto().getSelectedAnswer("missing")
        assertNull(result)
    }

    @Test
    fun `getSelectedAnswer returns null for JsonArray value`() {
        val result = dto("key" to JsonArray(listOf(JsonPrimitive("a")))).getSelectedAnswer("key")
        assertNull(result)
    }

    @Test
    fun `getSelectedAnswers returns list from JsonArray`() {
        val result = dto(
            "key" to JsonArray(listOf(JsonPrimitive("a"), JsonPrimitive("b")))
        ).getSelectedAnswers("key")
        assertEquals(listOf("a", "b"), result)
    }

    @Test
    fun `getSelectedAnswers returns single-element list from JsonPrimitive`() {
        val result = dto("key" to JsonPrimitive("a")).getSelectedAnswers("key")
        assertEquals(listOf("a"), result)
    }

    @Test
    fun `getSelectedAnswers returns empty list for missing key`() {
        val result = dto().getSelectedAnswers("missing")
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `getSelectedAnswers returns empty list for JsonObject value`() {
        val result = dto("key" to JsonObject(emptyMap())).getSelectedAnswers("key")
        assertEquals(emptyList<String>(), result)
    }
}
