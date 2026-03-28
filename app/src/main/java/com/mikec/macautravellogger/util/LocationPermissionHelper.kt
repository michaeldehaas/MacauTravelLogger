package com.mikec.macautravellogger.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object LocationPermissionHelper {

    fun hasFineLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    fun hasBackgroundLocation(context: Context): Boolean {
        // ACCESS_BACKGROUND_LOCATION was introduced in Android 10 (API 29).
        // On older devices, foreground permission alone covers background use.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAllLocationPermissions(context: Context): Boolean =
        hasFineLocation(context) && hasBackgroundLocation(context)
}
