package ai.tour.guide.data.room.entity

import ai.tour.guide.network.schema.response.ReceivedRoutePOI
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutePOITest {

    @Test
    fun `fromReceivedPoi serializes photos list to JSON and maps all fields`() {
        val received = ReceivedRoutePOI(
            name = "Tower",
            desc = "A tower",
            lat = 1.0,
            lng = 2.0,
            photos = listOf("url1")
        )

        val poi = RoutePOI.fromReceivedPoi(received, sessionId = 1, stopId = 2, poiIndex = 0)

        assertEquals("Tower", poi.name)
        assertEquals("A tower", poi.desc)
        assertEquals(1.0, poi.lat, 0.0)
        assertEquals(2.0, poi.lng, 0.0)
        assertEquals(1, poi.sessionId)
        assertEquals(2, poi.stopId)
        assertEquals(0, poi.poiIndex)
        assertEquals(listOf("url1"), poi.photosList())
    }

    @Test
    fun `photosList deserializes valid JSON`() {
        val poi = RoutePOI(name = "X", desc = "", lat = 0.0, lng = 0.0, photos = """["a","b"]""")
        assertEquals(listOf("a", "b"), poi.photosList())
    }

    @Test
    fun `photosList returns empty list for invalid JSON`() {
        val poi = RoutePOI(name = "X", desc = "", lat = 0.0, lng = 0.0, photos = "not-json")
        assertEquals(emptyList<String>(), poi.photosList())
    }

    @Test
    fun `photosList returns empty list for empty JSON array`() {
        val poi = RoutePOI(name = "X", desc = "", lat = 0.0, lng = 0.0, photos = "[]")
        assertEquals(emptyList<String>(), poi.photosList())
    }
}
