package com.mikec.macautravellogger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [TravelEntry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(TravelDatabase.Converters::class)
abstract class TravelDatabase : RoomDatabase() {

    abstract fun travelEntryDao(): TravelEntryDao

    class Converters {
        @TypeConverter
        fun fromDetectionMethod(method: DetectionMethod): String = method.name

        @TypeConverter
        fun toDetectionMethod(value: String): DetectionMethod = DetectionMethod.valueOf(value)
    }

    companion object {
        const val DATABASE_NAME = "travel_logger_db"
    }
}
