package com.mikec.macautravellogger.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object DateUtils {
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

    fun getCurrentDate(): String = LocalDate.now().format(DATE_FORMATTER)

    fun getCurrentYearMonth(): String = YearMonth.now().format(YEAR_MONTH_FORMATTER)

    fun getDaysInMonth(yearMonth: String): Int =
        YearMonth.parse(yearMonth, YEAR_MONTH_FORMATTER).lengthOfMonth()

    fun previousMonth(yearMonth: String): String =
        YearMonth.parse(yearMonth, YEAR_MONTH_FORMATTER)
            .minusMonths(1)
            .format(YEAR_MONTH_FORMATTER)

    fun nextMonth(yearMonth: String): String =
        YearMonth.parse(yearMonth, YEAR_MONTH_FORMATTER)
            .plusMonths(1)
            .format(YEAR_MONTH_FORMATTER)

    fun isCurrentOrPastMonth(yearMonth: String): Boolean =
        YearMonth.parse(yearMonth, YEAR_MONTH_FORMATTER) <= YearMonth.now()

    fun getCurrentTime(): String = LocalTime.now().format(TIME_FORMATTER)

    /** First day of the rolling 6-month window (today − 6 months, inclusive). */
    fun rollingWindowStart(): LocalDate = LocalDate.now().minusMonths(6)

    /** Formats a [LocalDate] to the "yyyy-MM-dd" string used by the database. */
    fun formatDate(date: LocalDate): String = date.format(DATE_FORMATTER)

    /** Returns duration in hours between two "HH:mm" strings. Handles crossing midnight. */
    fun calculateDurationHours(checkIn: String, checkOut: String): Double {
        val (inH, inM) = checkIn.split(":").map { it.toInt() }
        val (outH, outM) = checkOut.split(":").map { it.toInt() }
        val inMinutes = inH * 60 + inM
        var outMinutes = outH * 60 + outM
        if (outMinutes < inMinutes) outMinutes += 24 * 60
        return (outMinutes - inMinutes) / 60.0
    }
}
