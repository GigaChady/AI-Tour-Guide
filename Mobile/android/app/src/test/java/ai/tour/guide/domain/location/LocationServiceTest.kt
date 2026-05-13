package ai.tour.guide.domain.location

import ai.tour.guide.data.room.AppDatabase
import android.Manifest
import android.app.Application
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LocationServiceTest {

    private lateinit var context: Application
    private lateinit var appDatabase: AppDatabase
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var service: LocationService

    @Before
    fun setUp() {
        mockkStatic(LocationServices::class)
        fusedLocationClient = mockk(relaxed = true)
        every { LocationServices.getFusedLocationProviderClient(any<android.content.Context>()) } returns fusedLocationClient

        context = ApplicationProvider.getApplicationContext()
        appDatabase = mockk(relaxed = true)
        service = LocationService(context, appDatabase)
    }

    @After
    fun tearDown() {
        unmockkStatic(LocationServices::class)
    }

    @Test
    fun `hasLocationPermission returns false when permission not granted`() {
        assertFalse(service.hasLocationPermission())
    }

    @Test
    fun `hasLocationPermission returns true when ACCESS_FINE_LOCATION is granted`() {
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        assertTrue(service.hasLocationPermission())
    }

    @Test
    fun `startTracking exits early without calling fusedLocationClient when no permission`() {
        service.startTracking()

        verify(exactly = 0) {
            fusedLocationClient.requestLocationUpdates(any(), any<LocationCallback>(), any())
        }
    }

    @Test
    fun `getLastKnownLocation returns null when permission is not granted`() = runTest {
        val result = service.getLastKnownLocation()

        assertNull(result)
    }

    @Test
    fun `getLastKnownLocation returns cached location when already set`() = runTest {
        val mockLocation = mockk<Location>()
        val field = LocationService::class.java.getDeclaredField("_lastLocation")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(service) as MutableStateFlow<Location?>
        flow.value = mockLocation

        Shadows.shadowOf(context).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        val result = service.getLastKnownLocation()

        assertSame(mockLocation, result)
    }

    @Test
    fun `startTracking calls requestLocationUpdates when permission is granted`() {
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        service.startTracking()

        verify(exactly = 1) {
            fusedLocationClient.requestLocationUpdates(any(), any<LocationCallback>(), any())
        }
    }

    @Test
    fun `stopTracking calls fusedLocationClient removeLocationUpdates`() {
        service.stopTracking()

        verify(exactly = 1) { fusedLocationClient.removeLocationUpdates(any<LocationCallback>()) }
    }
}
