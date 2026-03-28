package com.mikec.macautravellogger.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Geofence radius
        item {
            SettingsCard {
                Text("Geofence Radius", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${settings.geofenceRadius.toInt()} m",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = settings.geofenceRadius,
                    onValueChange = { viewModel.updateGeofenceRadius(it) },
                    valueRange = 100f..5000f,
                    steps = 48,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("100 m", style = MaterialTheme.typography.labelSmall)
                    Text("5000 m", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        item { HorizontalDivider() }

        // Compliance threshold
        item {
            SettingsCard {
                Text(
                    "Annual Compliance Threshold (days/year)",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(8.dp))

                var text by remember(settings.complianceThresholdDays) {
                    mutableStateOf(settings.complianceThresholdDays.toString())
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        text = input
                        input.toIntOrNull()?.coerceIn(1, 365)
                            ?.let { viewModel.updateComplianceThresholdDays(it) }
                    },
                    label = { Text("Days per year") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Default: 183 days (Macau residency requirement)") }
                )
            }
        }

        item { HorizontalDivider() }

        // Rolling window max days
        item {
            SettingsCard {
                Text(
                    "Rolling 6-Month Maximum (days)",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(8.dp))

                var text by remember(settings.rollingWindowMaxDays) {
                    mutableStateOf(settings.rollingWindowMaxDays.toString())
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        text = input
                        input.toIntOrNull()?.coerceIn(1, 183)
                            ?.let { viewModel.updateRollingWindowMaxDays(it) }
                    },
                    label = { Text("Max days per rolling 6-month window") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Default: 45 days") }
                )
            }
        }

        item { HorizontalDivider() }

        // Notifications toggle
        item {
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notifications", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Alerts when approaching geofence limits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
