package ai.tour.guide.domain.location

import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.data.room.entity.RoutePositionHistory
import ai.tour.guide.domain.location.LocationService.Companion.FRESH_LOCATION_TIMEOUT_MS
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single

@Single
class LocationService(private val context: Context, private val appDatabase: AppDatabase) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _lastLocation = MutableStateFlow<Location?>(null)
    val appDbScope = CoroutineScope(Dispatchers.IO)

    private fun storeLocationInDb(location: Location) {
        appDbScope.launch {
            appDatabase.routePosHistoryDao().insertForLastSession(
                RoutePositionHistory(
                    lat = location.latitude,
                    lng = location.longitude
                )
            )
        }
    }

    private val trackingListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val current = _lastLocation.value
            // Only update if the new fix is more recent
            if (current == null || location.time >= current.time) {
                _lastLocation.value = location
                storeLocationInDb(location)
            }
        }

        @Deprecated("Deprecated in Android framework")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts the persistent background listener and seeds the internal cache from
     * Android's system cache. Call this once after location permission is granted.
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startTracking(
        minTimeMs: Long = DEFAULT_MIN_TIME_MS,
        minDistanceMeters: Float = DEFAULT_MIN_DISTANCE_METERS,
    ) {
        if (!hasLocationPermission()) return

        // Seed cache immediately from whatever Android already has
        getLocationProviders(enabledOnly = true)
            .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
            .maxByOrNull { it.time }
            ?.let { cached ->
                if (_lastLocation.value == null || cached.time > (_lastLocation.value?.time ?: 0)) {
                    _lastLocation.value = cached
                }
            }

        // Register persistent listener on all available providers
        getLocationProviders().forEach { provider ->
            locationManager.requestLocationUpdates(
                provider,
                minTimeMs,
                minDistanceMeters,
                trackingListener,
                Looper.getMainLooper()
            )
        }
    }

    /** Stops the persistent listener. Call this when the feature is torn down. */
    fun stopTracking() {
        locationManager.removeUpdates(trackingListener)
    }

    /**
     * Returns the best available location:
     * 1. Internal cache (updated by [startTracking] listener)
     * 2. Android system cache (last known per provider)
     * 3. One-shot fresh fix with a [FRESH_LOCATION_TIMEOUT_MS] timeout (cold-start fallback)
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null

        _lastLocation.value?.let { return it }

        val systemCached = getLocationProviders(enabledOnly = true)
            .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
            .maxByOrNull { it.time }

        if (systemCached != null) {
            _lastLocation.value = systemCached
            return systemCached
        }

        return awaitFreshLocation()
    }

    /**
     * Registers a one-shot listener and suspends until the first location fix arrives,
     * or returns null after [timeoutMs] milliseconds. Used as a last-resort fallback
     * when both the internal cache and system cache are empty (cold start).
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private suspend fun awaitFreshLocation(
        timeoutMs: Long = FRESH_LOCATION_TIMEOUT_MS
    ): Location? = withTimeoutOrNull(timeoutMs) {
        callbackFlow {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    _lastLocation.value = location
                    trySend(location)
                    channel.close()
                }

                @Deprecated("Deprecated in Android framework")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }

            getLocationProviders(enabledOnly = true).forEach { provider ->
                locationManager.requestLocationUpdates(
                    provider,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }

            awaitClose { locationManager.removeUpdates(listener) }
        }.firstOrNull()
    }

    private fun getLocationProviders(enabledOnly: Boolean = false): List<String> {
        val preferredProviders = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        return preferredProviders.filter { provider ->
            locationManager.allProviders.contains(provider) &&
                    (!enabledOnly || locationManager.isProviderEnabled(provider))
        }
    }

    private companion object {
        const val DEFAULT_MIN_TIME_MS = 10_000L
        const val DEFAULT_MIN_DISTANCE_METERS = 10f
        const val FRESH_LOCATION_TIMEOUT_MS = 10_000L
    }
}
