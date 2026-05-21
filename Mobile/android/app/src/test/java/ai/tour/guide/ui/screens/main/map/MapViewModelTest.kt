package ai.tour.guide.ui.screens.main.map

import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.data.room.dao.RoutePositionHistoryDao
import ai.tour.guide.data.room.dao.RouteSessionDao
import ai.tour.guide.data.room.dao.RouteStopDao
import ai.tour.guide.data.room.entity.RoutePositionHistory
import ai.tour.guide.data.room.entity.RouteSession
import ai.tour.guide.data.route.RouteStopDto
import ai.tour.guide.domain.location.LocationService
import ai.tour.guide.domain.route.RouteService
import android.app.Application
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertFalse
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
class MapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appDatabase: AppDatabase
    private lateinit var routeService: RouteService
    private lateinit var locationService: LocationService

    private lateinit var sessionDao: RouteSessionDao
    private lateinit var historyDao: RoutePositionHistoryDao
    private lateinit var stopDao: RouteStopDao

    private lateinit var viewModel: MapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        appDatabase = mockk(relaxed = true)
        routeService = mockk(relaxed = true)
        locationService = mockk(relaxed = true)

        sessionDao = mockk(relaxed = true)
        historyDao = mockk(relaxed = true)
        stopDao = mockk(relaxed = true)

        every { appDatabase.routeSessionDao() } returns sessionDao
        every { appDatabase.routePosHistoryDao() } returns historyDao
        every { appDatabase.routeStopDao() } returns stopDao

        every { routeService.currentSessionIdFlow } returns MutableStateFlow(null)

        viewModel = MapViewModel(appDatabase, routeService, locationService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onStart sets permission granted state to true when permission exists`() = runTest {
        every { locationService.hasLocationPermission() } returns true

        viewModel.onStart()
        advanceUntilIdle()

        assertTrue(viewModel.viewStateFlow.value.data.isLocationPermissionGranted)
    }

    @Test
    fun `onStart updates lastKnownLocation when permission is granted`() = runTest {
        every { locationService.hasLocationPermission() } returns true
        val mockLocation = mockk<Location>()
        every { mockLocation.latitude } returns 51.107883
        every { mockLocation.longitude } returns 17.038538
        coEvery { locationService.getLastKnownLocation() } returns mockLocation

        viewModel.onStart()
        advanceUntilIdle()

        val expectedLatLng = LatLng(51.107883, 17.038538)
        assertEquals(expectedLatLng, viewModel.viewStateFlow.value.data.lastKnownLocation)
    }

    @Test
    fun `onStart ignores location update when permission is denied (Guard Clause)`() = runTest {
        every { locationService.hasLocationPermission() } returns false

        viewModel.onStart()
        advanceUntilIdle()

        assertFalse(viewModel.viewStateFlow.value.data.isLocationPermissionGranted)
        assertNull(viewModel.viewStateFlow.value.data.lastKnownLocation)
        coVerify(exactly = 0) { locationService.getLastKnownLocation() }
    }

    @Test
    fun `updateLocationState safely handles SecurityException`() = runTest {
        every { locationService.hasLocationPermission() } returns true
        coEvery { locationService.getLastKnownLocation() } throws SecurityException("No permission")

        viewModel.onStart()
        advanceUntilIdle()

        assertNull(viewModel.viewStateFlow.value.data.lastKnownLocation)
    }

    @Test
    fun `observeRouteSession clears map when serverSessionId is null`() = runTest {
        val sessionFlow = MutableStateFlow<String?>("active-session")
        every { routeService.currentSessionIdFlow } returns sessionFlow

        val mockSession = mockk<RouteSession>()
        every { mockSession.id } returns 1
        coEvery { sessionDao.getSessionByServerId("active-session") } returns mockSession

        val historyList = listOf(RoutePositionHistory(lat = 1.0, lng = 1.0, sessionId = 1))
        every { historyDao.getHistoryForSession(1) } returns flowOf(historyList)
        every { stopDao.getStopMarkersForSession(1) } returns flowOf(emptyList())

        viewModel.onStart()
        advanceUntilIdle()

        assertTrue(viewModel.viewStateFlow.value.data.userPath.isNotEmpty())

        sessionFlow.value = null
        advanceUntilIdle()

        assertTrue(viewModel.viewStateFlow.value.data.userPath.isEmpty())
        assertTrue(viewModel.viewStateFlow.value.data.visitedStops.isEmpty())
    }

    @Test
    fun `observeRouteSession combines history and stops to build map state`() = runTest {
        val serverSessionId = "server-session-123"
        val sessionId = 1
        val sessionFlow = MutableStateFlow<String?>(serverSessionId)
        every { routeService.currentSessionIdFlow } returns sessionFlow

        val mockSession = mockk<RouteSession>()
        every { mockSession.id } returns sessionId
        coEvery { sessionDao.getSessionByServerId(serverSessionId) } returns mockSession

        val historyList = listOf(
            RoutePositionHistory(lat = 50.0, lng = 20.0, sessionId = sessionId),
            RoutePositionHistory(lat = 50.1, lng = 20.1, sessionId = sessionId)
        )

        val stopsList = listOf(
            RouteStopDto(stopId = 1, title = "ValidStopTitle", snippet = "ValidStopSnippet", latitude = 50.0, longitude = 20.0),
            RouteStopDto(stopId = 2, title = "InvalidStopTitle", snippet = "InvalidStopSnippet", latitude = null, longitude = null)
        )

        every { historyDao.getHistoryForSession(sessionId) } returns flowOf(historyList)
        every { stopDao.getStopMarkersForSession(sessionId) } returns flowOf(stopsList)

        viewModel.onStart()
        advanceUntilIdle()

        val state = viewModel.viewStateFlow.value.data

        assertEquals(2, state.userPath.size)
        assertEquals(LatLng(50.0, 20.0), state.userPath[0])

        assertEquals(1, state.visitedStops.size)
        assertEquals("1", state.visitedStops[0].id)
        assertEquals("ValidStopTitle", state.visitedStops[0].title)
        assertEquals(LatLng(50.0, 20.0), state.visitedStops[0].position)
    }

    @Test
    fun `onLocationPermissionResult updates state correctly`() = runTest {
        viewModel.onLocationPermissionResult(true)
        advanceUntilIdle()
        assertTrue(viewModel.viewStateFlow.value.data.isLocationPermissionGranted)

        viewModel.onLocationPermissionResult(false)
        advanceUntilIdle()
        assertFalse(viewModel.viewStateFlow.value.data.isLocationPermissionGranted)
    }
}