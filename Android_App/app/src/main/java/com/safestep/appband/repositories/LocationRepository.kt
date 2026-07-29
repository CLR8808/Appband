package com.safestep.appband.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*
import com.safestep.appband.models.DeviceCoordinates
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio de Geolocalización GPS.
 * Utiliza FusedLocationProviderClient oficial de Google Play Services.
 * Migrado desde services/location.ts
 */
class LocationRepository(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacionActual(): Result<DeviceCoordinates> {
        return try {
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()

            if (location != null) {
                Result.success(
                    DeviceCoordinates(
                        lat = location.latitude,
                        lng = location.longitude,
                        accuracy = location.accuracy,
                        timestamp = location.time
                    )
                )
            } else {
                Result.failure(Exception("No se pudo obtener la ubicación GPS"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    fun seguimientoUbicacionFlow(): Flow<DeviceCoordinates> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(
                        DeviceCoordinates(
                            lat = location.latitude,
                            lng = location.longitude,
                            accuracy = location.accuracy,
                            timestamp = location.time
                        )
                    )
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
