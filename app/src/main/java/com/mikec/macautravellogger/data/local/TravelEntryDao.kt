package com.mikec.macautravellogger.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TravelEntry): Long

    @Update
    suspend fun update(entry: TravelEntry)

    @Query("SELECT * FROM travel_entries WHERE substr(date, 1, 7) = :yearMonth ORDER BY date ASC")
    fun getByMonth(yearMonth: String): Flow<List<TravelEntry>>

    @Query("SELECT * FROM travel_entries ORDER BY date DESC")
    fun getAll(): Flow<List<TravelEntry>>

    @Query("SELECT * FROM travel_entries WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): TravelEntry?

    @Query("SELECT * FROM travel_entries WHERE isActive = 1 LIMIT 1")
    fun getActiveFlow(): Flow<TravelEntry?>

    @Query(
        "SELECT COUNT(DISTINCT date) FROM travel_entries " +
        "WHERE checkOutTime IS NOT NULL AND date >= :start AND date <= :end"
    )
    fun getDaysInRange(start: String, end: String): Flow<Int>

    @Query(
        "SELECT * FROM travel_entries WHERE checkOutTime IS NOT NULL " +
        "ORDER BY date DESC, checkOutTime DESC LIMIT :limit"
    )
    fun getRecent(limit: Int): Flow<List<TravelEntry>>
}
