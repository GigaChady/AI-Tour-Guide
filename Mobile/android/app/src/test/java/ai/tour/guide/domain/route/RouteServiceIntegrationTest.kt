package ai.tour.guide.domain.route

import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.domain.AppEventBus
import ai.tour.guide.domain.location.LocationService
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.schema.response.NarrationResponseDto
import ai.tour.guide.network.schema.response.NarrationTranscriptChunkDto
import ai.tour.guide.network.schema.response.ReceivedRoutePOI
import ai.tour.guide.network.schema.response.RoutePOIDto
import ai.tour.guide.network.ws.ServerEvent
import ai.tour.guide.network.ws.WSClient
import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for RouteService that use a real in-memory Room database.
 * WS callbacks are captured from the mocked WSClient and invoked directly,
 * verifying that service logic correctly persists data to the real DB.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RouteServiceIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var wsClient: WSClient
    private lateinit var service: RouteService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        wsClient = mockk(relaxed = true)
        val eventBus = AppEventBus()
        val locationService = mockk<LocationService>(relaxed = true)
        every { locationService.hasLocationPermission() } returns false
        val routeAudioRepository = mockk<RouteAudioRepository>(relaxed = true)
        val apiClient = mockk<ApiClient>(relaxed = true)
        val appDataRepository = mockk<AppDataRepository>(relaxed = true)
        every { appDataRepository.bearerTokenFlow } returns MutableStateFlow(null)

        service = RouteService(
            appDataRepository = appDataRepository,
            apiClient = apiClient,
            wsClient = wsClient,
            eventBus = eventBus,
            locationService = locationService,
            appDatabase = db,
            appEventBus = eventBus,
            routeAudioRepository = routeAudioRepository
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `session_start WS event persists RouteSession to database`() = runTest {
        val sessionSlot = slot<suspend (ServerEvent.SessionUpdated) -> Unit>()
        every { wsClient.onSessionUpdated(capture(sessionSlot)) } just Runs

        service.onStart()
        sessionSlot.captured.invoke(ServerEvent.SessionUpdated("sess-abc"))

        val cursor = db.openHelper.readableDatabase.query(
            "SELECT server_session_id FROM sessions WHERE server_session_id = ?",
            arrayOf("sess-abc")
        )
        assertTrue(cursor.moveToFirst())
        assertEquals("sess-abc", cursor.getString(0))
        cursor.close()
    }

    @Test
    fun `session_start WS event emits sessionId on currentSessionIdFlow`() = runTest {
        val sessionSlot = slot<suspend (ServerEvent.SessionUpdated) -> Unit>()
        every { wsClient.onSessionUpdated(capture(sessionSlot)) } just Runs

        service.onStart()
        sessionSlot.captured.invoke(ServerEvent.SessionUpdated("sess-xyz"))

        assertEquals("sess-xyz", service.currentSessionIdFlow.value)
    }

    @Test
    fun `narration_transcript WS event creates RouteStop with narration text in database`() = runTest {
        val sessionSlot = slot<suspend (ServerEvent.SessionUpdated) -> Unit>()
        every { wsClient.onSessionUpdated(capture(sessionSlot)) } just Runs
        val transcriptSlot = slot<suspend (NarrationResponseDto) -> Unit>()
        every { wsClient.onNarrationTranscript(capture(transcriptSlot)) } just Runs

        service.onStart()
        sessionSlot.captured.invoke(ServerEvent.SessionUpdated("sess-1"))
        transcriptSlot.captured.invoke(
            NarrationResponseDto(
                type = "narration_transcript",
                narrationId = "narr-1",
                transcript = listOf(NarrationTranscriptChunkDto(chunkId = 1, text = "Hello World"))
            )
        )

        val stop = db.routeStopDao().getByServerId("narr-1")
        assertNotNull(stop)
        assertEquals("Hello World", stop!!.narrationString)
    }

    @Test
    fun `pois WS event inserts RoutePOIs into database`() = runTest {
        val sessionSlot = slot<suspend (ServerEvent.SessionUpdated) -> Unit>()
        every { wsClient.onSessionUpdated(capture(sessionSlot)) } just Runs
        val poisSlot = slot<suspend (RoutePOIDto) -> Unit>()
        every { wsClient.onRoutePOIsReceived(capture(poisSlot)) } just Runs

        service.onStart()
        sessionSlot.captured.invoke(ServerEvent.SessionUpdated("sess-2"))
        poisSlot.captured.invoke(
            RoutePOIDto(
                type = "pois",
                narrationId = "narr-2",
                data = listOf(
                    ReceivedRoutePOI(name = "Tower", desc = "Old tower", lat = 1.0, lng = 2.0, photos = listOf("url1")),
                    ReceivedRoutePOI(name = "Museum", desc = "Art museum", lat = 3.0, lng = 4.0, photos = emptyList())
                )
            )
        )

        val cursor = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM pois", emptyArray())
        cursor.moveToFirst()
        assertEquals(2, cursor.getInt(0))
        cursor.close()
    }

    @Test
    fun `onDestroy after session_start resets currentSessionIdFlow to null`() = runTest {
        val sessionSlot = slot<suspend (ServerEvent.SessionUpdated) -> Unit>()
        every { wsClient.onSessionUpdated(capture(sessionSlot)) } just Runs

        service.onStart()
        sessionSlot.captured.invoke(ServerEvent.SessionUpdated("sess-3"))
        assertEquals("sess-3", service.currentSessionIdFlow.value)

        service.onDestroy()

        assertNull(service.currentSessionIdFlow.value)
    }
}
