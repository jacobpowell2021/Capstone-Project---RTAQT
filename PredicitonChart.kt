package com.example.airqualitytracker

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.common.model.Point
import com.example.airqualitytracker.ui.theme.Maroon

enum class PredMetric(
    val label: String,
    val color: Color,
    val yRange: ClosedFloatingPointRange<Float>
) {
    Temperature("Temperature (°F)", Color.Red,    0f..100f),
    Humidity   ("Humidity (%)",     Color.Blue,   0f..100f),
    Flammable  ("Flammable Gas",    Color.Blue,   0f..1f),
    TVOC       ("TVOC",             Color.Green,  0f..1f),
    CO         ("CO",               Color.Yellow, 0f..1f)
}

@Composable
fun LoadingPredictionOverlay(
    message: String = "Loading…",
    spinnerColor: Color = Maroon
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = spinnerColor, strokeWidth = 5.dp)
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionChartsSection(vm: PredictionChartViewModel = viewModel()) {
    var daysText by remember { mutableStateOf("1.0") }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(PredMetric.Temperature) }

    // map dropdown selection -> VM points
    fun pointsFor(metric: PredMetric): List<Point> = when (metric) {
        PredMetric.Temperature -> vm.tempPoints
        PredMetric.Humidity    -> vm.humidityPoints
        PredMetric.Flammable   -> vm.flammablePoints
        PredMetric.TVOC        -> vm.tvocPoints
        PredMetric.CO          -> vm.coPoints
    }

    Box(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row: days input + fetch
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it },
                    label = { Text("days") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White,
                        cursorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        focusedContainerColor = Maroon,
                        unfocusedContainerColor = Maroon
                    ),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { daysText.toFloatOrNull()?.let(vm::fetchAndBuild) },
                    enabled = !vm.isLoading.value
                ) {
                    Text(if (vm.isLoading.value) "Loading…" else "Fetch")
                }
            }

            // Error message
            vm.error.value?.let { errorMsg ->
                Text(
                    "Error: $errorMsg",
                    color = Color(0xFFFFB4A9),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Only show chart controls if we have data
            if (vm.tempPoints.isNotEmpty() || vm.humidityPoints.isNotEmpty()) {
                // Dropdown to choose which chart to display
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        readOnly = true,
                        value = selected.label,
                        onValueChange = {},
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        label = { Text("Select metric", color = Color.White) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Maroon,
                            unfocusedContainerColor = Maroon,
                            focusedIndicatorColor = Color.White,
                            unfocusedIndicatorColor = Color.White,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White,
                            cursorColor = Color.White,
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF121212))
                    ) {
                        PredMetric.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option.label,
                                        color = if (option == selected) Color.Cyan else Color.White
                                    )
                                },
                                onClick = {
                                    selected = option
                                    expanded = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = Color.White,
                                    disabledTextColor = Color.Gray
                                )
                            )
                        }
                    }
                }

                // Crossfade between charts for smoother UX
                Crossfade(targetState = selected, label = "predMetricCrossfade") { metric ->
                    val points = pointsFor(metric)
                    if (points.isNotEmpty()) {
                        MetricLineChart(
                            title = metric.label,
                            points = points,
                            color = metric.color,
                            fixedYRange = metric.yRange,
                            xLabels = vm.xLabels,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        )
                    } else {
                        Text(
                            "No data available for ${metric.label}",
                            color = Color.Gray,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            } else if (!vm.isLoading.value) {
                // No data and not loading - show instruction
                Text(
                    "Enter number of days and click Fetch to load predictions",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        }

        // Loading overlay on top
        if (vm.isLoading.value) {
            LoadingPredictionOverlay(
                message = "Building prediction charts…",
                spinnerColor = Color.Cyan
            )
        }
    }
}