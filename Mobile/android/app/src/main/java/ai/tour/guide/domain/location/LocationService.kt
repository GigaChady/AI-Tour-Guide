package ai.tour.guide.domain.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.koin.core.annotation.Single

@Single
class LocationService(private val context: Context) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        return getLocationProviders()
            .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
            .maxByOrNull { location -> location.time }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun observeLocationUpdates(
        minTimeMs: Long = DEFAULT_MIN_TIME_MS,
        minDistanceMeters: Float = DEFAULT_MIN_DISTANCE_METERS,
        emitLastKnownLocation: Boolean = true
    ): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }

            @Deprecated("Deprecated in Android framework")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        val providers = getLocationProviders()
        providers.forEach { provider ->
            locationManager.requestLocationUpdates(
                provider,
                minTimeMs,
                minDistanceMeters,
                listener,
                Looper.getMainLooper()
            )
        }

        if (emitLastKnownLocation) {
            getLastKnownLocation()?.let { lastLocation ->
                trySend(lastLocation)
            }
        }

        awaitClose {
            locationManager.removeUpdates(listener)
        }
    }

    private fun getLocationProviders(): List<String> {
        val preferredProviders = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        return preferredProviders.filter { provider ->
            locationManager.allProviders.contains(provider)
        }
    }

    private companion object {
        const val DEFAULT_MIN_TIME_MS = 5_000L
        const val DEFAULT_MIN_DISTANCE_METERS = 5f
    }
}
