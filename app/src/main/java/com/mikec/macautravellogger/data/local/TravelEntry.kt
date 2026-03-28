package com.mikec.macautravellogger.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DetectionMethod {
    AUTO, MANUAL
}

@Entity(tableName = "travel_entries")
data class TravelEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,             // ISO format: "YYYY-MM-DD"
    val checkInTime: String?,     // "HH:mm"
    val checkOutTime: String?,    // "HH:mm"
    val durationHours: Double?,
    val detectionMethod: DetectionMethod,
    val notes: String?,
    val isActive: Boolean = false
)
