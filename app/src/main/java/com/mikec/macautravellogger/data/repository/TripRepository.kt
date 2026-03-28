package com.mikec.macautravellogger.data.repository

import com.mikec.macautravellogger.data.local.DetectionMethod
import com.mikec.macautravellogger.data.local.TravelEntry
import com.mikec.macautravellogger.data.local.TravelEntryDao
import com.mikec.macautravellogger.util.DateUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val dao: TravelEntryDao
) {

    fun getAll(): Flow<List<TravelEntry>> = dao.getAll()

    fun getByMonth(yearMonth: String): Flow<List<TravelEntry>> = dao.getByMonth(yearMonth)

    fun getActiveFlow(): Flow<TravelEntry?> = dao.getActiveFlow()

    suspend fun getActive(): TravelEntry? = dao.getActive()

    suspend fun checkIn(
        date: String,
        time: String,
        method: DetectionMethod,
        notes: String? = null
    ): Long {
        val entry = TravelEntry(
            date = date,
            checkInTime = time,
            checkOutTime = null,
            durationHours = null,
            detectionMethod = method,
            notes = notes,
            isActive = true
        )
        return dao.insert(entry)
    }

    suspend fun checkOut(entry: TravelEntry, checkOutTime: String) {
        val duration = if (entry.checkInTime != null) {
            DateUtils.calculateDurationHours(entry.checkInTime, checkOutTime)
        } else null
        dao.update(
            entry.copy(
                checkOutTime = checkOutTime,
                durationHours = duration,
                isActive = false
            )
        )
    }

    fun getRecentTrips(limit: Int = 3): Flow<List<TravelEntry>> = dao.getRecent(limit)

    /**
     * Count of distinct days with a completed trip in the rolling 6-month window
     * [today − 6 months, today], inclusive. Updates reactively with the database.
     */
    fun getRollingDayCount(): Flow<Int> {
        val start = DateUtils.formatDate(DateUtils.rollingWindowStart())
        val end = DateUtils.getCurrentDate()
        return dao.getDaysInRange(start, end)
    }

    /**
     * Updates an entry and recalculates durationHours whenever both checkIn and
     * checkOut are present. If either time is missing, duration is set to null.
     */
    suspend fun updateEntry(entry: TravelEntry) {
        val duration = if (entry.checkInTime != null && entry.checkOutTime != null) {
            DateUtils.calculateDurationHours(entry.checkInTime, entry.checkOutTime)
        } else null
        dao.update(entry.copy(durationHours = duration))
    }
}
