package ai.tour.guide.domain.location

import ai.tour.guide.data.room.AppDatabase
import ai.tour.guide.data.room.entity.RoutePositionHistory
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.annotation.Single
import kotlin.coroutines.resume

@Single
class LocationService(private val context: Context, private val appDatabase: AppDatabase) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _lastLocation = MutableStateFlow<Location?>(null)
    private val appDbScope = CoroutineScope(Dispatchers.IO)

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

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                val current = _lastLocation.value
                if (current == null || location.time > current.time) {
                    _lastLocation.value = location
                    storeLocationInDb(location)
                }
            }
        }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startTracking(
        minTimeMs: Long = DEFAULT_MIN_TIME_MS,
        minDistanceMeters: Float = DEFAULT_MIN_DISTANCE_METERS,
    ) {
        if (!hasLocationPermission()) return

        // Seed cache immediately from GMS Last Location
        fusedLocationClient.lastLocation.addOnSuccessListener { cached ->
            if (cached != null) {
                val current = _lastLocation.value
                if (current == null || cached.time > current.time) {
                    _lastLocation.value = cached
                }
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, minTimeMs)
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null

        _lastLocation.value?.let { return it }

        return suspendCancellableCoroutine<Location?> { cont ->
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (cont.isActive) {
                            if (location != null) {
                                _lastLocation.value = location
                            }
                            cont.resume(location)
                        }
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            } catch (e: SecurityException) {
                Log.e(TAG, "Error when getting current location", e)
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private companion object {
        const val TAG = "LocationService"
        const val DEFAULT_MIN_TIME_MS = 10_000L
        const val DEFAULT_MIN_DISTANCE_METERS = 10f
    }
}
