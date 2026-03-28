package com.mikec.macautravellogger.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikec.macautravellogger.data.preferences.UserPreferencesRepository
import com.mikec.macautravellogger.data.preferences.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettings()
        )

    fun updateGeofenceRadius(radius: Float) {
        viewModelScope.launch {
            preferencesRepository.updateGeofenceRadius(radius)
        }
    }

    fun updateComplianceThresholdDays(days: Int) {
        viewModelScope.launch {
            preferencesRepository.updateComplianceThresholdDays(days)
        }
    }

    fun updateRollingWindowMaxDays(days: Int) {
        viewModelScope.launch {
            preferencesRepository.updateRollingWindowMaxDays(days)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateNotificationsEnabled(enabled)
        }
    }
}
