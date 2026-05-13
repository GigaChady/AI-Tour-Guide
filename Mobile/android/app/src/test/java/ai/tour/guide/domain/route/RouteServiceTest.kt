package ai.tour.guide.domain.route

import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.domain.AppEventBus
import ai.tour.guide.domain.location.LocationService
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.ws.WSClient
import ai.tour.guide.network.ws.WSClientRoute
import android.app.Application
import android.location.Location
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RouteServiceTest {

    private lateinit var appDataRepository: AppDataRepository
    private lateinit var apiClient: ApiClient
    private lateinit var wsClient: WSClient
    private lateinit var eventBus: AppEventBus
    private lateinit var locationService: LocationService
    private lateinit var appDatabase: AppDatabase
    private lateinit var routeAudioRepository: RouteAudioRepository
    private lateinit var service: RouteService

    @Before
    fun setUp() {
        appDataRepository = mockk(relaxed = true)
        every { appDataRepository.bearerTokenFlow } returns MutableStateFlow(null)
        apiClient = mockk(relaxed = true)
        wsClient = mockk(relaxed = true)
        eventBus = mockk(relaxed = true)
        locationService = mockk(relaxed = true)
        appDatabase = mockk(relaxed = true)
        routeAudioRepository = mockk(relaxed = true)

        service = RouteService(
            appDataRepository = appDataRepository,
            apiClient = apiClient,
            wsClient = wsClient,
            eventBus = eventBus,
            locationService = locationService,
            appDatabase = appDatabase,
            appEventBus = eventBus,
            routeAudioRepository = routeAudioRepository
        )
    }

    @Test
    fun `currentSessionIdFlow starts as null`() {
        assertNull(service.currentSessionIdFlow.value)
    }

    @Test
    fun `sendLastKnownLocation sends lat-lng payload via wsClient when location is available`() = runTest {
        val location = mockk<Location>()
        every { location.latitude } returns 51.5
        every { location.longitude } returns -0.1
        coEvery { locationService.getLastKnownLocation() } returns location

        service.sendLastKnownLocation()

        coVerify(exactly = 1) { wsClient.send(any()) }
    }

    @Test
    fun `sendLastKnownLocation does not call wsClient when location is null`() = runTest {
        coEvery { locationService.getLastKnownLocation() } returns null

        service.sendLastKnownLocation()

        coVerify(exactly = 0) { wsClient.send(any()) }
    }

    @Test
    fun `sendLastKnownLocation catches SecurityException and does not send`() = runTest {
        coEvery { locationService.getLastKnownLocation() } throws SecurityException("Permission denied")

        service.sendLastKnownLocation()

        coVerify(exactly = 0) { wsClient.send(any()) }
    }

    @Test
    fun `onDestroy calls wsClient onDestroy`() = runTest {
        service.onDestroy()

        verify(exactly = 1) { wsClient.onDestroy() }
    }

    @Test
    fun `onDestroy calls locationService stopTracking`() = runTest {
        service.onDestroy()

        verify(exactly = 1) { locationService.stopTracking() }
    }

    @Test
    fun `onDestroy calls routeAudioRepository clearSession`() = runTest {
        service.onDestroy()

        coVerify(exactly = 1) { routeAudioRepository.clearSession() }
    }

    @Test
    fun `onDestroy resets currentSessionIdFlow to null`() = runTest {
        service.onDestroy()

        assertNull(service.currentSessionIdFlow.value)
    }

    @Test
    fun `onStart calls locationService startTracking when permission is granted`() = runTest {
        every { locationService.hasLocationPermission() } returns true

        service.onStart()

        verify(exactly = 1) { locationService.startTracking() }
    }

    @Test
    fun `onStart does not call locationService startTracking when permission is not granted`() = runTest {
        every { locationService.hasLocationPermission() } returns false

        service.onStart()

        verify(exactly = 0) { locationService.startTracking() }
    }

    @Test
    fun `onStart calls wsClient connect`() = runTest {
        service.onStart()

        coVerify(exactly = 1) { wsClient.connect(WSClientRoute.ROUTE) }
    }

    @Test
    fun `onStart sends bearer token payload via wsClient when token is available`() = runTest {
        every { appDataRepository.bearerTokenFlow } returns MutableStateFlow("my-token")
        // Recreate service so it picks up the new stub for bearerTokenFlow
        service = RouteService(
            appDataRepository = appDataRepository,
            apiClient = apiClient,
            wsClient = wsClient,
            eventBus = eventBus,
            locationService = locationService,
            appDatabase = appDatabase,
            appEventBus = eventBus,
            routeAudioRepository = routeAudioRepository
        )

        service.onStart()

        coVerify(atLeast = 1) { wsClient.send(any()) }
    }
}
