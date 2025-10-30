package com.example.airqualitytracker

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.common.model.Point
import com.example.airqualitytracker.ui.theme.Maroon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatestChartsSection(vm: LatestChartViewModel = viewModel()) {
    // 2) Dropdown state
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(Metric.Temperature) }

    // 3) Helper to map metric -> points list from VM
    fun pointsFor(metric: Metric): List<Point> = when (metric) {
        Metric.Temperature -> vm.tempPoints
        Metric.Humidity    -> vm.humidityPoints
        Metric.Flammable    -> vm.flammablePoints
        Metric.TVOC        -> vm.tvocPoints
        Metric.CO          -> vm.coPoints
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        // --- Exposed dropdown to choose chart ---
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
                label = { Text("Select metric") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors = TextFieldDefaults.textFieldColors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    containerColor = Maroon,                  // Background
                    focusedIndicatorColor = Color.White,           // Underline when focused
                    unfocusedIndicatorColor = Color.White,         // Underline when not focused
                    focusedLabelColor = Color.White,               // Label color when active
                    unfocusedLabelColor = Color.White,          //label color when inactive
                    cursorColor = Color.White,
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Metric.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            selected = option
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- Smoothly swap charts when selection changes ---
        Crossfade(targetState = selected, label = "metricCrossfade") { metric ->
            MetricLineChart(
                title = metric.label,
                points = pointsFor(metric),
                color = metric.color,
                fixedYRange = metric.yRange,
                xLabels = vm.xLabels,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
        }
    }
}
enum class Metric(
    val label: String,
    val color: Color,
    val yRange: ClosedFloatingPointRange<Float>
) {
    Temperature("Temperature (°F)", Color.Red,   0f..100f),
    Humidity   ("Humidity (%)",     Color.Blue,  0f..100f),
    Flammable  ("Flammable Gas",    Color.Blue,  0f..1f),
    TVOC       ("TVOC",             Color.Green, 0f..110f),
    CO         ("CO",               Color.Yellow,0f..5f)
}