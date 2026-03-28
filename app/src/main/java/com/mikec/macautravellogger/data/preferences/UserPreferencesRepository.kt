package com.mikec.macautravellogger.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class UserSettings(
    val geofenceRadius: Float = DEFAULT_GEOFENCE_RADIUS,
    val complianceThresholdDays: Int = DEFAULT_COMPLIANCE_DAYS,
    val rollingWindowMaxDays: Int = DEFAULT_ROLLING_MAX_DAYS,
    val notificationsEnabled: Boolean = true
) {
    companion object {
        const val DEFAULT_GEOFENCE_RADIUS = 100f   // metres
        const val DEFAULT_COMPLIANCE_DAYS = 183    // days per year (Macau residency requirement)
        const val DEFAULT_ROLLING_MAX_DAYS = 45    // max days in any rolling 6-month window
    }
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_GEOFENCE_RADIUS = floatPreferencesKey("geofence_radius")
        private val KEY_COMPLIANCE_DAYS = intPreferencesKey("compliance_threshold_days")
        private val KEY_ROLLING_MAX_DAYS = intPreferencesKey("rolling_window_max_days")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val settingsFlow: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            geofenceRadius = prefs[KEY_GEOFENCE_RADIUS] ?: UserSettings.DEFAULT_GEOFENCE_RADIUS,
            complianceThresholdDays = prefs[KEY_COMPLIANCE_DAYS] ?: UserSettings.DEFAULT_COMPLIANCE_DAYS,
            rollingWindowMaxDays = prefs[KEY_ROLLING_MAX_DAYS] ?: UserSettings.DEFAULT_ROLLING_MAX_DAYS,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
        )
    }

    suspend fun updateGeofenceRadius(radius: Float) {
        dataStore.edit { prefs -> prefs[KEY_GEOFENCE_RADIUS] = radius }
    }

    suspend fun updateComplianceThresholdDays(days: Int) {
        dataStore.edit { prefs -> prefs[KEY_COMPLIANCE_DAYS] = days }
    }

    suspend fun updateRollingWindowMaxDays(days: Int) {
        dataStore.edit { prefs -> prefs[KEY_ROLLING_MAX_DAYS] = days }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }
}
