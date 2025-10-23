// TemperatureChartSection.kt
package com.example.airqualitytracker
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine

//@Composable
/*fun PredictionTemperatureChartSection(vm: PredictionChartViewModel = viewModel()) {
    var daysText by remember { mutableStateOf("3.0") }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = daysText,
            onValueChange = { daysText = it },
            label = { Text("days") },
            singleLine = true
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

    if (vm.tempPoints.isNotEmpty()) {
        Log.d("PredictionTemperatureChart", vm.tempPoints.toString())
        val yMin = 60
        val yMax = 100

        val yAxis = AxisData.Builder()
            .steps(5)
            .labelData { i: Int ->
                val v = yMin + (yMax - yMin) * (i / 5f)
                String.format("%.1f", v)
            }
            .build()

        val xAxis = AxisData.Builder()
            .steps(6)
            .build()

        // your real line
        val realLine = Line(
            dataPoints = vm.tempPoints,
            lineStyle = LineStyle(color = Color.Cyan) // pick a visible color
        )

// invisible helper line to stretch y-axis
        val anchorLine = Line(
            dataPoints = listOf(Point(0f, 60f), Point(0f, 100f)),
            lineStyle = LineStyle(color = Color.Transparent),
            intersectionPoint = IntersectionPoint(color = Color.Transparent),
            selectionHighlightPoint = SelectionHighlightPoint(color = Color.Transparent),
            shadowUnderLine = ShadowUnderLine(color = Color.Transparent),
            selectionHighlightPopUp = SelectionHighlightPopUp(backgroundColor = Color.Transparent)
        )

        val chartData = LineChartData(
            linePlotData = LinePlotData(lines = listOf(realLine, anchorLine)),
            xAxisData = xAxis,
            yAxisData = yAxis
        )
        Spacer(Modifier.height(12.dp))
        LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            lineChartData = chartData
        )
    }
}
*/