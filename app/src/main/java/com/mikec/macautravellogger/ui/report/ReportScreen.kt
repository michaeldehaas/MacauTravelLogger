package com.mikec.macautravellogger.ui.report

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikec.macautravellogger.data.local.TravelEntry
import com.mikec.macautravellogger.ui.components.MonthNavigator

@Composable
fun ReportScreen(viewModel: ReportViewModel = hiltViewModel()) {
    val summary by viewModel.summary.collectAsState()
    val yearMonth by viewModel.selectedYearMonth.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.exportEvent.collect { event ->
            when (event) {
                is ExportEvent.Success -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                }
                is ExportEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MonthNavigator(
                yearMonth = yearMonth,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth
            )
        }

        item { ReportSummaryCards(summary) }

        if (summary.entries.isNotEmpty()) {
            item {
                Text("Trip Breakdown", style = MaterialTheme.typography.titleMedium)
            }
            item { HorizontalDivider() }
            items(summary.entries, key = { it.id }) { entry ->
                TripBreakdownItem(entry)
            }
        } else {
            item {
                Text(
                    "No trips recorded for this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(Modifier.height(4.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = viewModel::exportToCsv,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.TableChart, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Export CSV")
                }
                Button(
                    onClick = viewModel::exportToPdf,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Export PDF")
                }
            }
        }

        item {
            TextButton(
                onClick = { shareSummary(context, summary) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Share Summary")
            }
        }
    }
}

@Composable
private fun ReportSummaryCards(summary: MonthlySummary) {
    val complianceColor = when {
        summary.compliancePercent >= 0.5f -> Color(0xFF4CAF50)
        summary.compliancePercent >= 0.35f -> Color(0xFFFFC107)
        else -> MaterialTheme.colorScheme.error
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportCard("Days",  summary.uniqueDayCount.toString(), Modifier.weight(1f))
        ReportCard("Trips", summary.tripCount.toString(),      Modifier.weight(1f))
        ReportCard("Hours", "${"%.1f".format(summary.totalHours)}h", Modifier.weight(1f))
        ReportCard(
            label = "Comply",
            value = "${(summary.compliancePercent * 100).toInt()}%",
            modifier = Modifier.weight(1f),
            valueColor = complianceColor
        )
    }
}

@Composable
private fun ReportCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TripBreakdownItem(entry: TravelEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(entry.date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "${entry.checkInTime ?: "—"} → ${entry.checkOutTime ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!entry.notes.isNullOrBlank()) {
                    Text(entry.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    entry.durationHours?.let { "${"%.1f".format(it)}h" } ?: "—",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    entry.detectionMethod.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun shareSummary(context: android.content.Context, summary: MonthlySummary) {
    val text = buildString {
        appendLine("Macau Travel Report — ${summary.yearMonth}")
        appendLine("Days in Macau: ${summary.uniqueDayCount}")
        appendLine("Total trips: ${summary.tripCount}")
        appendLine("Total hours: ${"%.1f".format(summary.totalHours)}")
        appendLine("Monthly compliance: ${(summary.compliancePercent * 100).toInt()}%")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, "Macau Travel Report — ${summary.yearMonth}")
    }
    context.startActivity(Intent.createChooser(intent, null))
}
