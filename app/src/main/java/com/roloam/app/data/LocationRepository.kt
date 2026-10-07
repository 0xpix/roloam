package com.roloam.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.roloam.app.model.GeoPoint
import kotlinx.coroutines.tasks.await

data class Origin(val label: String, val point: GeoPoint, val isFallback: Boolean)

class LocationRepository(private val context: Context) {
    private val fused = LocationServices.getFusedLocationProviderClient(context)

    suspend fun currentOrigin(): Origin {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            return heidelberg()
        }
        return runCatching {
            val token = CancellationTokenSource()
            val location = fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token).await()
                ?: fused.lastLocation.await()
                ?: return@runCatching heidelberg()
            Origin("Current location", GeoPoint(location.latitude, location.longitude), false)
        }.getOrElse { heidelberg() }
    }

    private fun heidelberg() = Origin("Heidelberg", GeoPoint(49.3988, 8.6724), true)
}
