package ai.tour.guide.ui.sharedFragments.tourSummary

import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.data.room.dao.RoutePositionHistoryDao
import ai.tour.guide.data.room.dao.RouteSessionDao
import ai.tour.guide.data.room.dao.RouteStopDao
import ai.tour.guide.data.room.entity.RoutePositionHistory
import ai.tour.guide.data.room.entity.RouteSession
import ai.tour.guide.domain.route.RouteNarrationPlaybackService
import ai.tour.guide.domain.route.RoutePlaybackState
import android.app.Application
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TourRouteSummaryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appDatabase: AppDatabase
    private lateinit var routeAudioService: RouteNarrationPlaybackService

    private lateinit var sessionDao: RouteSessionDao
    private lateinit var historyDao: RoutePositionHistoryDao
    private lateinit var stopDao: RouteStopDao

    private lateinit var viewModel: TourRouteSummaryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        appDatabase = mockk(relaxed = true)
        routeAudioService = mockk(relaxed = true)

        sessionDao = mockk(relaxed = true)
        historyDao = mockk(relaxed = true)
        stopDao = mockk(relaxed = true)

        every { appDatabase.routeSessionDao() } returns sessionDao
        every { appDatabase.routePosHistoryDao() } returns historyDao
        every { appDatabase.routeStopDao() } returns stopDao

        every { routeAudioService.playbackStateFlow } returns MutableStateFlow(RoutePlaybackState())

        viewModel = TourRouteSummaryViewModel(appDatabase, routeAudioService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `observeLatestSession clears state when no session available`() = runTest {
        val mockSession = RouteSession(
            id = 1,
            serverSessionId = "server123",
            createdAt = System.currentTimeMillis() - 60000
        )
        val sessionFlow = MutableStateFlow<RouteSession?>(mockSession)
        every { sessionDao.getLatestSessionFlow() } returns sessionFlow

        every { historyDao.getHistoryForSession(1) } returns flowOf(emptyList())
        every { stopDao.getStopMarkersForSession(1) } returns flowOf(emptyList())
        every { stopDao.getLatestStopIdForServerSession("server123") } returns flowOf(null)

        viewModel.onStart()
        advanceUntilIdle()

        assertEquals("1 min", viewModel.viewStateFlow.value.data.durationText)

        sessionFlow.value = null
        advanceUntilIdle()

        assertEquals("-", viewModel.viewStateFlow.value.data.durationText)
        assertTrue(viewModel.viewStateFlow.value.data.visitedPlaces.isEmpty())
    }

    @Test
    fun `buildSummaryState freezes duration and nullifies activeStopId when session is ended`() = runTest {
        val serverSessionId = "server123"
        val sessionId = 1

        val mockSession = RouteSession(id = sessionId, serverSessionId = serverSessionId, createdAt = 10000L, endedAt = 70000L)

        val sessionFlow = MutableStateFlow<RouteSession?>(mockSession)
        every { sessionDao.getLatestSessionFlow() } returns sessionFlow

        every { historyDao.getHistoryForSession(sessionId) } returns flowOf(emptyList())
        every { stopDao.getStopMarkersForSession(sessionId) } returns flowOf(emptyList())

        every { stopDao.getLatestStopIdForServerSession(serverSessionId) } returns flowOf(5)

        viewModel.onStart()
        advanceUntilIdle()

        val state = viewModel.viewStateFlow.value.data

        assertEquals("1 min", state.durationText)

        assertNull(state.activeStopId)
    }

    @Test
    fun `buildSummaryState calculates running duration when session is ongoing`() = runTest {
        val sessionId = 1
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60000)

        val mockSession = RouteSession(id = sessionId, serverSessionId = "server123", createdAt = fiveMinutesAgo, endedAt = null)

        val sessionFlow = MutableStateFlow<RouteSession?>(mockSession)
        every { sessionDao.getLatestSessionFlow() } returns sessionFlow

        every { historyDao.getHistoryForSession(sessionId) } returns flowOf(emptyList())
        every { stopDao.getStopMarkersForSession(sessionId) } returns flowOf(emptyList())
        every { stopDao.getLatestStopIdForServerSession("server123") } returns flowOf(2)

        viewModel.onStart()
        advanceUntilIdle()

        val state = viewModel.viewStateFlow.value.data

        assertEquals("5 min", state.durationText)
        assertEquals(2, state.activeStopId)
    }

    @Test
    fun `calculateTotalDistance formats distance properly`() = runTest {
        val sessionId = 1
        val mockSession = RouteSession(id = sessionId, serverSessionId = "server123")
        every { sessionDao.getLatestSessionFlow() } returns flowOf(mockSession)

        val historyList = listOf(
            RoutePositionHistory(lat = 51.107, lng = 17.038, sessionId = sessionId),
            RoutePositionHistory(lat = 52.406, lng = 16.925, sessionId = sessionId)
        )

        every { historyDao.getHistoryForSession(sessionId) } returns flowOf(historyList)
        every { stopDao.getStopMarkersForSession(sessionId) } returns flowOf(emptyList())
        every { stopDao.getLatestStopIdForServerSession("server123") } returns flowOf(null)

        viewModel.onStart()
        advanceUntilIdle()

        val state = viewModel.viewStateFlow.value.data

        assertTrue(state.distanceText.matches(Regex("\\d+\\.\\d km")))
    }
}