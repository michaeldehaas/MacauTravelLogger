package com.mikec.macautravellogger.ui.history

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikec.macautravellogger.data.local.TravelEntry
import com.mikec.macautravellogger.ui.components.MonthNavigator

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val entries by viewModel.entries.collectAsState()
    val yearMonth by viewModel.selectedYearMonth.collectAsState()
    val filter by viewModel.filter.collectAsState()

    val completed = entries.filter { it.checkOutTime != null }
    val uniqueDays = completed.map { it.date }.toSet().size
    val totalHours = completed.sumOf { it.durationHours ?: 0.0 }

    var expandedEntryId by rememberSaveable { mutableLongStateOf(-1L) }

    Column(modifier = Modifier.fillMaxSize()) {
        MonthNavigator(
            yearMonth = yearMonth,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Summary cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard("Trips", completed.size.toString(), Modifier.weight(1f))
            SummaryCard("Days", uniqueDays.toString(), Modifier.weight(1f))
            SummaryCard("Hours", "${"%.1f".format(totalHours)}h", Modifier.weight(1f))
        }

        // Filter pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TripFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { viewModel.setFilter(f) },
                    label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        if (entries.isEmpty()) {
            Text(
                "No trips recorded for this month.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    EditableEntryItem(
                        entry = entry,
                        isExpanded = expandedEntryId == entry.id,
                        onToggle = {
                            expandedEntryId = if (expandedEntryId == entry.id) -1L else entry.id
                        },
                        onSave = { updated ->
                            viewModel.updateEntry(updated)
                            expandedEntryId = -1L
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableEntryItem(
    entry: TravelEntry,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSave: (TravelEntry) -> Unit
) {
    var checkIn by remember(entry.id) { mutableStateOf(entry.checkInTime ?: "") }
    var checkOut by remember(entry.id) { mutableStateOf(entry.checkOutTime ?: "") }
    var notes by remember(entry.id) { mutableStateOf(entry.notes ?: "") }
    var showCheckInPicker by remember { mutableStateOf(false) }
    var showCheckOutPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row header — tappable to expand
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        "${entry.checkInTime ?: "—"} → ${entry.checkOutTime ?: "—"}  " +
                            (entry.durationHours?.let { "${"%.1f".format(it)}h" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        entry.detectionMethod.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            // Inline edit panel
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = checkIn,
                            onValueChange = { checkIn = it },
                            label = { Text("Check In") },
                            placeholder = { Text("HH:mm") },
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(onClick = { showCheckInPicker = true }) {
                                    Icon(Icons.Filled.ExpandMore, contentDescription = "Pick time")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = checkOut,
                            onValueChange = { checkOut = it },
                            label = { Text("Check Out") },
                            placeholder = { Text("HH:mm") },
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(onClick = { showCheckOutPicker = true }) {
                                    Icon(Icons.Filled.ExpandMore, contentDescription = "Pick time")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { if (it.length <= 100) notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        supportingText = { Text("${notes.length}/100") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { onSave(entry) }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            onSave(
                                entry.copy(
                                    checkInTime = checkIn.ifBlank { null },
                                    checkOutTime = checkOut.ifBlank { null },
                                    notes = notes.ifBlank { null }
                                )
                            )
                        }) { Text("Save") }
                    }
                }
            }
        }
    }

    if (showCheckInPicker) {
        TimePickerDialog(
            title = "Check-In Time",
            initialHour = parseHour(checkIn),
            initialMinute = parseMinute(checkIn),
            onDismiss = { showCheckInPicker = false },
            onConfirm = { time ->
                checkIn = time
                showCheckInPicker = false
            }
        )
    }

    if (showCheckOutPicker) {
        TimePickerDialog(
            title = "Check-Out Time",
            initialHour = parseHour(checkOut),
            initialMinute = parseMinute(checkOut),
            onDismiss = { showCheckOutPicker = false },
            onConfirm = { time ->
                checkOut = time
                showCheckOutPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm("%02d:%02d".format(state.hour, state.minute))
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun parseHour(time: String) = time.split(":").getOrNull(0)?.toIntOrNull() ?: 8
private fun parseMinute(time: String) = time.split(":").getOrNull(1)?.toIntOrNull() ?: 0
