package com.mikec.macautravellogger.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikec.macautravellogger.data.local.TravelEntry
import com.mikec.macautravellogger.util.LocationPermissionHelper

@Composable
fun HomeScreen(viewModel: TripViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasFine by remember { mutableStateOf(LocationPermissionHelper.hasFineLocation(context)) }
    var hasBackground by remember { mutableStateOf(LocationPermissionHelper.hasBackgroundLocation(context)) }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasFine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                  permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackground = granted
    }

    // Auto-start geofencing once both permissions are present
    LaunchedEffect(hasFine, hasBackground) {
        if (hasFine && hasBackground && !state.geofencingActive) {
            viewModel.startGeofencing()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!hasFine) {
            item {
                LocationPermissionCard(
                    title = "Location Permission Required",
                    description = "Grant precise location access so the app can detect when you enter or leave Macau automatically.",
                    buttonLabel = "Grant Location",
                    onGrant = {
                        fineLocationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }
        } else if (!hasBackground) {
            item {
                LocationPermissionCard(
                    title = "Background Location Required",
                    description = "Allow location access \"All the time\" so geofencing can detect border crossings even when the app is closed.",
                    buttonLabel = "Grant Background Location",
                    onGrant = {
                        backgroundLocationLauncher.launch(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                    }
                )
            }
        }

        item { StatusCard(state) }
        item { CheckInOutButton(state.activeTrip != null, viewModel::checkIn, viewModel::checkOut) }
        item { MetricCardsRow(state) }
        item { RollingWindowCard(state.rollingDays) }

        if (state.recentTrips.isNotEmpty()) {
            item {
                Text(
                    "Recent Trips",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(state.recentTrips, key = { it.id }) { entry ->
                RecentTripItem(entry)
            }
        }
    }
}

@Composable
private fun LocationPermissionCard(
    title: String,
    description: String,
    buttonLabel: String,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF57F17),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF57F17)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5D4037)
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17))
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun StatusCard(state: HomeUiState) {
    val isActive = state.activeTrip != null
    val containerColor = if (isActive) Color(0xFFD7F5DC) else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isActive) "Currently in Macau" else "Not in Macau",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            val trip = state.activeTrip
            if (trip != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Checked in: ${trip.checkInTime ?: "—"} on ${trip.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32)
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (state.geofencingActive) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (state.geofencingActive) "Geofencing active" else "Geofencing inactive",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.geofencingActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CheckInOutButton(
    isActive: Boolean,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit
) {
    if (isActive) {
        OutlinedButton(
            onClick = onCheckOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Check Out")
        }
    } else {
        Button(onClick = onCheckIn, modifier = Modifier.fillMaxWidth()) {
            Text("Check In (Manual)")
        }
    }
}

@Composable
private fun MetricCardsRow(state: HomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard("Trips", state.monthTripCount.toString(), Modifier.weight(1f))
        MetricCard("Days", state.monthDayCount.toString(), Modifier.weight(1f))
        MetricCard(
            "Month",
            "${(state.monthCompliancePercent * 100).toInt()}%",
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RollingWindowCard(rollingDays: Int) {
    val max = 45
    val progress = (rollingDays.toFloat() / max).coerceIn(0f, 1f)
    val (trackColor, statusText) = when {
        rollingDays <= 35 -> Color(0xFF4CAF50) to "Within safe limit"
        rollingDays <= 42 -> Color(0xFFFFC107) to "Approaching limit"
        rollingDays < max -> Color(0xFFFF5722) to "Near maximum — take care"
        else              -> MaterialTheme.colorScheme.error to "At maximum limit"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rolling 6-Month Window", style = MaterialTheme.typography.titleSmall)
                Text(
                    "$rollingDays / $max days",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = trackColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(statusText, style = MaterialTheme.typography.bodySmall, color = trackColor)
        }
    }
}

@Composable
private fun RecentTripItem(entry: TravelEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
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
            }
            Text(
                entry.durationHours?.let { "${"%.1f".format(it)}h" } ?: "—",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
