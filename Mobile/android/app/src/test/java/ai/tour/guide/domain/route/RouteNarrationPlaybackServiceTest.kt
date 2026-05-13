package ai.tour.guide.domain.route

import ai.tour.guide.domain.AppEventBus
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RouteNarrationPlaybackServiceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var routeAudioRepository: RouteAudioRepository
    private lateinit var eventBus: AppEventBus
    private lateinit var service: RouteNarrationPlaybackService

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Application>()
        routeAudioRepository = mockk(relaxed = true)
        eventBus = mockk(relaxed = true)
        service = RouteNarrationPlaybackService(context, routeAudioRepository, eventBus)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isPlayingFlow starts as false`() {
        assertFalse(service.isPlayingFlow.value)
    }

    @Test
    fun `playbackStateFlow starts as default RoutePlaybackState`() {
        assertEquals(RoutePlaybackState(), service.playbackStateFlow.value)
    }

    @Test
    fun `pauseNarration sets isPlaying to false`() = runTest {
        service.pauseNarration()
        advanceUntilIdle()

        assertFalse(service.isPlayingFlow.value)
    }

    @Test
    fun `pauseNarration works before player is initialized`() = runTest {
        // No playAudioFile called — player is null; pauseNarration must not crash
        service.pauseNarration()
        advanceUntilIdle()

        assertFalse(service.isPlayingFlow.value)
    }

    @Test
    fun `onDestroy sets isPlaying to false`() = runTest {
        service.onDestroy()
        advanceUntilIdle()

        assertFalse(service.isPlayingFlow.value)
    }

    @Test
    fun `onDestroy resets playbackState to default`() = runTest {
        service.onDestroy()
        advanceUntilIdle()

        assertEquals(RoutePlaybackState(), service.playbackStateFlow.value)
    }

    @Test
    fun `onDestroy calls routeAudioRepository clearSession`() = runTest {
        service.onDestroy()
        advanceUntilIdle()

        coVerify(exactly = 1) { routeAudioRepository.clearSession() }
    }
}
