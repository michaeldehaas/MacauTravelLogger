package com.mikec.macautravellogger.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikec.macautravellogger.data.local.DetectionMethod
import com.mikec.macautravellogger.data.local.TravelEntry
import com.mikec.macautravellogger.data.repository.TripRepository
import com.mikec.macautravellogger.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TripFilter { ALL, AUTO, MANUAL }

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel @Inject constructor(
    private val repository: TripRepository
) : ViewModel() {

    private val _selectedYearMonth = MutableStateFlow(DateUtils.getCurrentYearMonth())
    val selectedYearMonth: StateFlow<String> = _selectedYearMonth.asStateFlow()

    private val _filter = MutableStateFlow(TripFilter.ALL)
    val filter: StateFlow<TripFilter> = _filter.asStateFlow()

    val entries: StateFlow<List<TravelEntry>> = combine(
        _selectedYearMonth.flatMapLatest { repository.getByMonth(it) },
        _filter
    ) { allEntries, currentFilter ->
        when (currentFilter) {
            TripFilter.ALL -> allEntries
            TripFilter.AUTO -> allEntries.filter { it.detectionMethod == DetectionMethod.AUTO }
            TripFilter.MANUAL -> allEntries.filter { it.detectionMethod == DetectionMethod.MANUAL }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun selectMonth(yearMonth: String) {
        _selectedYearMonth.value = yearMonth
    }

    fun previousMonth() {
        _selectedYearMonth.value = DateUtils.previousMonth(_selectedYearMonth.value)
    }

    fun nextMonth() {
        val next = DateUtils.nextMonth(_selectedYearMonth.value)
        if (DateUtils.isCurrentOrPastMonth(next)) {
            _selectedYearMonth.value = next
        }
    }

    fun setFilter(filter: TripFilter) {
        _filter.value = filter
    }

    fun updateEntry(entry: TravelEntry) {
        viewModelScope.launch {
            repository.updateEntry(entry)
        }
    }
}
