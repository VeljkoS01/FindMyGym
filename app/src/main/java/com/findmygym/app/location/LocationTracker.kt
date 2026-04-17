package com.findmygym.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationTracker(context: Context) {

    //FusedLocationProviderClient je Google servis za dobijanje lokacije uredjaja
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun locationUpdates(
        intervalMs: Long = 5_000,
        fastestIntervalMs: Long = 2_500,
        minDistanceM: Float = 10f
    ): Flow<android.location.Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(fastestIntervalMs)
            .setMinUpdateDistanceMeters(minDistanceM)
            .build()

        //Callback koji se poziva svaki put kada stigne nova lokacija
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(loc)
            }
        }

        //Pokretanje slusanja lokacijskih promena na glavnom thread-u
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        //Kada Flow vise nije potreban, uklanjamo callback da ne bi trosili resurse
        awaitClose { client.removeLocationUpdates(callback) }
    }
}