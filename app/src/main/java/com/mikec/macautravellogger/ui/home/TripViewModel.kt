package com.mikec.macautravellogger.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikec.macautravellogger.data.local.DetectionMethod
import com.mikec.macautravellogger.data.local.TravelEntry
import com.mikec.macautravellogger.data.repository.TripRepository
import com.mikec.macautravellogger.location.GeofenceManager
import com.mikec.macautravellogger.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val activeTrip: TravelEntry? = null,
    val monthTripCount: Int = 0,
    val monthDayCount: Int = 0,
    val monthCompliancePercent: Float = 0f,
    val currentYearMonth: String = DateUtils.getCurrentYearMonth(),
    val rollingDays: Int = 0,
    val recentTrips: List<TravelEntry> = emptyList(),
    val geofencingActive: Boolean = false
)

@HiltViewModel
class TripViewModel @Inject constructor(
    private val repository: TripRepository,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _geofencingActive = MutableStateFlow(false)

    init {
        val yearMonth = DateUtils.getCurrentYearMonth()
        viewModelScope.launch {
            combine(
                repository.getByMonth(yearMonth),
                repository.getActiveFlow(),
                repository.getRollingDayCount(),
                repository.getRecentTrips(3),
                _geofencingActive
            ) { monthEntries, activeTrip, rollingDays, recentTrips, geofencingActive ->
                val completed = monthEntries.filter { it.checkOutTime != null }
                val uniqueDays = completed.map { it.date }.toSet().size
                val daysInMonth = DateUtils.getDaysInMonth(yearMonth)
                HomeUiState(
                    activeTrip = activeTrip,
                    monthTripCount = completed.size,
                    monthDayCount = uniqueDays,
                    monthCompliancePercent = if (daysInMonth > 0) uniqueDays.toFloat() / daysInMonth else 0f,
                    currentYearMonth = yearMonth,
                    rollingDays = rollingDays,
                    recentTrips = recentTrips,
                    geofencingActive = geofencingActive
                )
            }.collect { _uiState.value = it }
        }
    }

    fun checkIn() {
        viewModelScope.launch {
            repository.checkIn(
                date = DateUtils.getCurrentDate(),
                time = DateUtils.getCurrentTime(),
                method = DetectionMethod.MANUAL
            )
        }
    }

    fun checkOut() {
        val activeTrip = _uiState.value.activeTrip ?: return
        viewModelScope.launch {
            repository.checkOut(activeTrip, DateUtils.getCurrentTime())
        }
    }

    fun startGeofencing() {
        viewModelScope.launch {
            geofenceManager.registerGeofence()
            _geofencingActive.update { true }
        }
    }
}
