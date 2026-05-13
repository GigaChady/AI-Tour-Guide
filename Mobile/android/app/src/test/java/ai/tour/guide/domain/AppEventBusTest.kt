package ai.tour.guide.domain

import android.app.Application
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppEventBusTest {

    @Test
    fun `publish emits AudioChunkReceived to eventsFlow`() = runTest(UnconfinedTestDispatcher()) {
        val bus = AppEventBus()
        val file = File("/tmp/test.mp3")
        val received = mutableListOf<AppEventBusEvent>()

        val job = launch { bus.eventsFlow.collect { received.add(it) } }
        bus.publish(AppEventBusEvent.AudioChunkReceived(file))

        assertEquals(1, received.size)
        assertEquals(AppEventBusEvent.AudioChunkReceived(file), received[0])
        job.cancel()
    }

    @Test
    fun `publish emits RouteSessionStarted to eventsFlow`() = runTest(UnconfinedTestDispatcher()) {
        val bus = AppEventBus()
        val received = mutableListOf<AppEventBusEvent>()

        val job = launch { bus.eventsFlow.collect { received.add(it) } }
        bus.publish(AppEventBusEvent.RouteSessionStarted("session-1"))

        assertEquals(AppEventBusEvent.RouteSessionStarted("session-1"), received[0])
        job.cancel()
    }

    @Test
    fun `publish emits AudioChunkNearlyFinished to eventsFlow`() =
        runTest(UnconfinedTestDispatcher()) {
            val bus = AppEventBus()
            val received = mutableListOf<AppEventBusEvent>()

            val job = launch { bus.eventsFlow.collect { received.add(it) } }
            bus.publish(AppEventBusEvent.AudioChunkNearlyFinished(1500L))

            assertEquals(AppEventBusEvent.AudioChunkNearlyFinished(1500L), received[0])
            job.cancel()
        }

    @Test
    fun `publish emits RouteTimeout to eventsFlow`() = runTest(UnconfinedTestDispatcher()) {
        val bus = AppEventBus()
        val received = mutableListOf<AppEventBusEvent>()

        val job = launch { bus.eventsFlow.collect { received.add(it) } }
        bus.publish(AppEventBusEvent.RouteTimeout("timeout reason"))

        assertEquals(AppEventBusEvent.RouteTimeout("timeout reason"), received[0])
        job.cancel()
    }

    @Test
    fun `multiple subscribers each receive the same event`() = runTest(UnconfinedTestDispatcher()) {
        val bus = AppEventBus()
        val received1 = mutableListOf<AppEventBusEvent>()
        val received2 = mutableListOf<AppEventBusEvent>()

        val job1 = launch { bus.eventsFlow.collect { received1.add(it) } }
        val job2 = launch { bus.eventsFlow.collect { received2.add(it) } }

        bus.publish(AppEventBusEvent.RouteSessionStarted("s1"))

        assertEquals(1, received1.size)
        assertEquals(1, received2.size)
        assertEquals(received1[0], received2[0])
        job1.cancel()
        job2.cancel()
    }

    @Test
    fun `replay is zero - late subscriber does not receive past events`() = runTest {
        val bus = AppEventBus()
        bus.publish(AppEventBusEvent.RouteTimeout(null))

        val received = mutableListOf<AppEventBusEvent>()
        val job = launch { bus.eventsFlow.collect { received.add(it) } }
        advanceUntilIdle()

        assertEquals(0, received.size)
        job.cancel()
    }
}
