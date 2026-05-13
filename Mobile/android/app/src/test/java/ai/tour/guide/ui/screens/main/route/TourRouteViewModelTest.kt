package ai.tour.guide.ui.screens.main.route

import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.data.room.dao.RouteStopDao
import ai.tour.guide.domain.AppEventBus
import ai.tour.guide.domain.route.RouteNarrationPlaybackService
import ai.tour.guide.domain.route.RoutePlaybackState
import ai.tour.guide.domain.route.RouteService
import android.app.Application
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TourRouteViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var routeAudioService: RouteNarrationPlaybackService
    private lateinit var routeService: RouteService
    private lateinit var appEventBus: AppEventBus
    private lateinit var appDatabase: AppDatabase
    private lateinit var routeStopDao: RouteStopDao
    private lateinit var viewModel: TourRouteViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        routeAudioService = mockk(relaxed = true)
        routeService = mockk(relaxed = true)
        appEventBus = mockk(relaxed = true)
        appDatabase = mockk(relaxed = true)
        routeStopDao = mockk(relaxed = true)

        every { routeAudioService.isPlayingFlow } returns MutableStateFlow(false)
        every { routeAudioService.playbackStateFlow } returns MutableStateFlow(RoutePlaybackState())
        every { routeService.currentSessionIdFlow } returns MutableStateFlow(null)
        every { appEventBus.eventsFlow } returns MutableSharedFlow()
        every { appDatabase.routeStopDao() } returns routeStopDao
        every { routeStopDao.getStopsCountUntilStopId(any()) } returns flowOf(null)
        every { routeStopDao.getStopIdByOffsetFromStop(any(), any()) } returns flowOf(null)
        every { routeStopDao.getStopIndexById(any()) } returns flowOf(null)
        every { routeStopDao.getStopById(any()) } returns flowOf(null)
        every { routeStopDao.getLatestStopIdForServerSession(any()) } returns flowOf(null)
        every { routeStopDao.narrationFilesExistsForCurrentSession(any()) } returns flowOf(false)

        viewModel = TourRouteViewModel(routeAudioService, routeService, appEventBus, appDatabase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isPlayingFlow mirrors routeAudioService isPlayingFlow`() {
        assertTrue(viewModel.isPlayingFlow === routeAudioService.isPlayingFlow)
    }

    @Test
    fun `onPlayClicked calls routeAudioService playNarration`() = runTest {
        viewModel.onPlayClicked()
        advanceUntilIdle()

        coVerify(exactly = 1) { routeAudioService.playNarration() }
    }

    @Test
    fun `onPauseClicked calls routeAudioService pauseNarration`() = runTest {
        viewModel.onPauseClicked()
        advanceUntilIdle()

        coVerify(exactly = 1) { routeAudioService.pauseNarration() }
    }

    @Test
    fun `onDestroy calls routeAudioService onDestroy`() = runTest {
        viewModel.onDestroy()
        advanceUntilIdle()

        coVerify(exactly = 1) { routeAudioService.onDestroy() }
    }

    @Test
    fun `onDestroy calls routeService onDestroy`() = runTest {
        viewModel.onDestroy()
        advanceUntilIdle()

        coVerify(exactly = 1) { routeService.onDestroy() }
    }

    @Test
    fun `onDestroy resets view state to default`() = runTest {
        viewModel.onDestroy()
        advanceUntilIdle()

        assertEquals(TourRouteState.default(), viewModel.viewStateFlow.value.data)
    }

    @Test
    fun `onStart calls routeService onStart`() = runTest {
        viewModel.onStart()
        advanceUntilIdle()

        coVerify(exactly = 1) { routeService.onStart() }
    }

    @Test
    fun `onNextClicked when offset is zero calls routeService sendLastKnownLocation`() = runTest {
        viewModel.onNextClicked()
        advanceUntilIdle()

        coVerify(exactly = 1) { routeService.sendLastKnownLocation() }
    }

    @Test
    fun `onPrevClicked does not change state when totalStops is null`() = runTest {
        viewModel.onPrevClicked()
        advanceUntilIdle()

        assertEquals(0, viewModel.viewStateFlow.value.data.currentHistoryOffset)
    }
}
