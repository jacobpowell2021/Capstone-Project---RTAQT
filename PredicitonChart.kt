package com.example.airqualitytracker

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.common.model.Point
import com.example.airqualitytracker.ui.theme.Maroon


// PredictionChart.kt
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
    spinnerColor: Color = Color.Cyan
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
    var daysText by remember { mutableStateOf("3.0") }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(PredMetric.Temperature) }

    // map dropdown selection -> VM points
    fun pointsFor(metric: PredMetric): List<Point> = when (metric) {
        PredMetric.Temperature -> vm.tempPoints
        PredMetric.Humidity    -> vm.humidityPoints
        PredMetric.Flammable    -> vm.flammablePoints
        PredMetric.TVOC        -> vm.tvocPoints
        PredMetric.CO          -> vm.coPoints
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .fillMaxWidth()
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
                    onClick = { daysText.toFloatOrNull()?.let(vm::fetchAndBuild) }
                ) { Text(if (vm.isLoading.value) "Loading…" else "Fetch") }
            }

            Spacer(Modifier.height(12.dp))
            /*
            // Debug info (optional)
            Text("points: ${vm.tempPoints.size}", color = Color.White)
            if (vm.tempPoints.isNotEmpty()) {
                val first = vm.tempPoints.first()
                val last  = vm.tempPoints.last()
                Text("first: x=${first.x}, y=${first.y}", color = Color.White)
                Text("last:  x=${last.x},  y=${last.y}",  color = Color.White)
            }
            vm.error.value?.let { Text("Error: $it", color = Color(0xFFFFB4A9)) }

            Spacer(Modifier.height(12.dp))
            */
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

            Spacer(Modifier.height(16.dp))

            // Crossfade between charts for smoother UX
            Crossfade(targetState = selected, label = "predMetricCrossfade") { metric ->
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

        // Loading overlay on top
        if (vm.isLoading.value) {
            LoadingPredictionOverlay(
                message = "Building prediction charts…",
                spinnerColor = Color.Cyan
            )
        }
    }
}


//Working Prediction Screen
/*package com.example.airqualitytracker

import com.example.airqualitytracker.PredictionChartViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.axis.AxisData
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import android.util.Log
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import com.example.airqualitytracker.ui.theme.Maroon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionChartsSection(vm: PredictionChartViewModel = viewModel()) {
    var daysText by remember { mutableStateOf("3.0") }
    // Remember scroll state
    val scrollState = rememberScrollState()
    Column (
        modifier = Modifier
            .verticalScroll(scrollState)

    ){
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = daysText,
                onValueChange = { daysText = it },
                label = { Text("days") },
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    containerColor = Maroon,                  // Background
                    focusedIndicatorColor = Color.White,           // Underline when focused
                    unfocusedIndicatorColor = Color.White,         // Underline when not focused
                    focusedLabelColor = Color.White,               // Label color when active
                    unfocusedLabelColor = Color.White,          //label color when inactive
                    cursorColor = Color.White,                      // Cursor color
                )

            )
            Button(onClick = { daysText.toFloatOrNull()?.let(vm::fetchAndBuild) }) {
                Text(if (vm.isLoading.value) "Loading…" else "Fetch")
            }

        }
        Text("points: ${vm.tempPoints.size}", color = Color.White)
        if (vm.tempPoints.isNotEmpty()) {
            val first = vm.tempPoints.first()
            val last  = vm.tempPoints.last()
            Text("first: x=${first.x}, y=${first.y}", color = Color.White)
            Text("last:  x=${last.x},  y=${last.y}",  color = Color.White)
        }


        vm.error.value?.let { Text("Error: $it", color = Color(0xFFFFB4A9)) }
// Temperature
        MetricLineChart(
            title = "Temperature (°F)",
            points = vm.tempPoints,
            color = Color.Red,
            fixedYRange = 60f..100f,                 // your request: force 60..100 while keeping line invisible
            xLabels = vm.xLabels,                    // or null if you don't want labels
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(Modifier.height(16.dp))
// Humidity
        MetricLineChart(
            title = "Humidity",
            points = vm.humidityPoints,
            color = Color.Blue,
            fixedYRange = 60f..100f,
            xLabels = vm.xLabels,                    // or null if you don't want labels
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(Modifier.height(16.dp))

// Flammable
        MetricLineChart(
            title = "Flammable Gas",
            points = vm.flammablePoints,
            color = Color.Blue,
            fixedYRange = 60f..100f,
            xLabels = vm.xLabels,                    // or null if you don't want labels
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(Modifier.height(16.dp))

// TVOC
        MetricLineChart(
            title = "TVOC",
            points = vm.tvocPoints,
            color = Color.Green,
            fixedYRange = 0f..1f,
            xLabels = vm.xLabels,                  // or null if you don't want labels
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(Modifier.height(16.dp))

//CO
        MetricLineChart(
            title = "CO",
            points = vm.coPoints,
            color = Color.Yellow,
            fixedYRange = 0f..1f,
            xLabels = vm.xLabels,                    // or null if you don't want labels
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
    }

}
*/