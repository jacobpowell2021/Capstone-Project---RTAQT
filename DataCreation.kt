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


//Charts that work 10/16/2025
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
fun LatestChartsSection(vm: LatestChartViewModel = viewModel()) {
    // Remember scroll state
    val scrollState = rememberScrollState()
    Column (
        modifier = Modifier
            .verticalScroll(scrollState)

    ){
// Temperature

        MetricLineChart(
            title = "Temperature (°F)",
            points = vm.tempPoints,
            color = Color.Red,
            fixedYRange = 0f..100f,
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
            fixedYRange = 0f..100f,
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
            fixedYRange = 0f..1f,
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
            fixedYRange = 0f..110f,
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
            fixedYRange = 0f..5f,
            xLabels = vm.xLabels,                    // or null if you don't want labels
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
    }

}
*/

/*package com.example.airqualitytracker

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment


@Composable
fun DataCreationScreen(/*navController: NavController*/) {
    // --- Activity-scoped VM persists across navigation ---
    val activity = LocalContext.current as ComponentActivity
    val vm: ChartViewModel = viewModel(activity)

    // Start MQTT once
    LaunchedEffect(Unit) {
        if (!vm.started) {
            MqttManager.startListening(vm.temperatureState, vm.humidityState, vm.particleState)
            vm.markStarted()
            Log.d("ChartObs", "MQTT listening started (activity-scoped VM)")
        }
    }

    // Append a point ONLY when humidity changes to a new value
    LaunchedEffect(vm.humidityState.value) {
        val now = Date()
        val time = SimpleDateFormat("HH:mm", Locale.US).format(now)   // or "h:mm a"
        val md   = SimpleDateFormat("MMM d", Locale.US).format(now)   // e.g., "Sep 2"
        val label = "$time $md"                                      // two-line label

        vm.appendHumidityIfNew(vm.humidityState.value, label)
    }

    // ---- Fixed Y axis 0..100 ----
    val steps = 5
    val yAxisData = AxisData.Builder()
        .steps(steps)
        .backgroundColor(Color.Black)
        .labelAndAxisLinePadding(20.dp)
        .labelData { i -> (i * (100 / steps)).toString() } // 0,20,40,60,80,100
        .axisLabelColor(Color.White)
        .build()

    // ---- X axis w/ horizontal scroll (optional) ----
    val stepDp = 80.dp
    val xSteps = (minOf(vm.pointsData.size, vm.dateLabels.size) - 1).coerceAtLeast(1)
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val contentWidth: Dp = maxOf(screenWidth, (xSteps * stepDp.value).dp)

    val xAxisData = AxisData.Builder()
        .axisStepSize(stepDp)
        .backgroundColor(Color.Black)
        .steps(xSteps)
        .labelData { i -> if (i in vm.dateLabels.indices) vm.dateLabels[i] else "" }
        .labelAndAxisLinePadding(15.dp)
        .axisLabelColor(Color.White)
        .build()

    // Invisible anchor line to lock Y range to [0,100]
    val anchorLine = Line(
        dataPoints = listOf(Point(0f, 0f), Point(0f, 100f)),
        lineStyle = LineStyle(color = Color.Transparent),
        intersectionPoint = IntersectionPoint(color = Color.Transparent),
        selectionHighlightPoint = SelectionHighlightPoint(color = Color.Transparent),
        shadowUnderLine = ShadowUnderLine(color = Color.Transparent),
        selectionHighlightPopUp = SelectionHighlightPopUp(backgroundColor = Color.Transparent)
    )

    val dataLine = Line(
        dataPoints = if (vm.pointsData.isEmpty()) listOf(Point(0f, 0f)) else vm.pointsData,
        lineStyle = LineStyle(color = Color.White),
        intersectionPoint = IntersectionPoint(color = Color.White),
        selectionHighlightPoint = SelectionHighlightPoint(color = Color.White),
        shadowUnderLine = ShadowUnderLine(),
        selectionHighlightPopUp = SelectionHighlightPopUp()
    )

    val lineChartData = LineChartData(
        linePlotData = LinePlotData(lines = listOf(anchorLine, dataLine)),
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        gridLines = GridLines(),
        backgroundColor = Color.Black
    )

    // Autoscroll to latest (optional)
    val hScroll = rememberScrollState()
    LaunchedEffect(vm.dateLabels.size) {
        hScroll.animateScrollTo(hScroll.maxValue)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .horizontalScroll(hScroll)
    ) {
        LineChart(
            modifier = Modifier
                .width(contentWidth)
                .height(300.dp),
            lineChartData = lineChartData
        )
    }
    Spacer(Modifier.height(12.dp))

    // Clear button
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Button(
            onClick = { vm.clearSeries() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Gray, // or Maroon
                contentColor = Color.Black   // or Color.White with Maroon
            )
        ) {
            Text("Clear Graph")
        }
    }
}
*/