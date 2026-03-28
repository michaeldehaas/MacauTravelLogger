package com.mikec.macautravellogger.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mikec.macautravellogger.util.DateUtils
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun MonthNavigator(
    yearMonth: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNextAllowed = DateUtils.isCurrentOrPastMonth(DateUtils.nextMonth(yearMonth))
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
        }
        Text(
            text = formatYearMonth(yearMonth),
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(onClick = onNext, enabled = isNextAllowed) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
        }
    }
}

private val YEAR_MONTH_IN = DateTimeFormatter.ofPattern("yyyy-MM")
private val YEAR_MONTH_OUT = DateTimeFormatter.ofPattern("MMMM yyyy")

private fun formatYearMonth(yearMonth: String): String = try {
    YearMonth.parse(yearMonth, YEAR_MONTH_IN).format(YEAR_MONTH_OUT)
} catch (e: Exception) {
    yearMonth
}
