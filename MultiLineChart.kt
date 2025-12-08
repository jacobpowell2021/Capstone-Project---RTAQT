// MultiLineChart.kt
package com.example.airqualitytracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import com.example.airqualitytracker.ui.theme.Black
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Reusable single-series line chart.
 * @param title Optional title text shown above the chart.
 * @param points The series to plot (x,y).
 * @param modifier Container modifier.
 * @param color Line color for the real series.
 * @param fixedYRange If provided, forces Y axis to start to end using an invisible anchor line.
 * @param xLabels Optional labels for X axis (indexed to your points); if null, numeric ticks are used.
 * @param allTimeLabels Optional complete list of time labels for popup (used when xLabels has gaps).
 * @param ySteps Number of Y axis steps (ticks).
 * @param xSteps Number of X axis steps (ticks).
 */

@Composable
fun MetricLineChart(
    title: String? = null,
    points: List<Point>,
    modifier: Modifier = Modifier,
    color: Color = Color.Cyan,
    fixedYRange: ClosedFloatingPointRange<Float>? = null,
    xLabels: List<String>? = null,
    allTimeLabels: List<String>? = null,
    ySteps: Int = 5,
    xSteps: Int = 24,
) {
    Column {
        if (!title.isNullOrBlank()) {
            Text(title, color = Color.White)
            Spacer(Modifier.height(8.dp))
        }

        if (points.isEmpty()) {
            Text("No data", color = Color.White.copy(alpha = 0.7f))
            return
        }

        // ----- Y range handling -----
        val (yMin, yMax) = if (fixedYRange != null) {
            fixedYRange.start to fixedYRange.endInclusive
        } else {
            val ys = points.map { it.y }
            var minY = ys.minOrNull() ?: 0f
            var maxY = ys.maxOrNull() ?: 0f
            // pad if flat so the line is visible
            if (abs(maxY - minY) < 1e-6f) {
                val pad = max(0.5f, abs(maxY) * 0.01f)
                minY -= pad
                maxY += pad
            }
            minY to maxY
        }

        val yAxis = AxisData.Builder()
            .steps(ySteps)
            .labelData { i: Int ->
                val v = yMin + (yMax - yMin) * (i / ySteps.toFloat())
                String.format("%.2f", v)
            }
            .build()

        // Updated X-axis configuration
        val xAxis = AxisData.Builder()
            .steps(xLabels?.size?.minus(1) ?: xSteps)
            .labelData { i ->
                xLabels?.getOrNull(i) ?: i.toString()
            }
            .labelAndAxisLinePadding(15.dp) // Add padding for multi-line labels
            //.axisLabelAngle(20f) // Slight angle to prevent overlap
            .build()

        // Real visible line with custom popup
        val realLine = Line(
            dataPoints = points,
            lineStyle = LineStyle(color = color),
            intersectionPoint = IntersectionPoint(radius = 4.dp, color = Black),
            selectionHighlightPopUp = SelectionHighlightPopUp(
                backgroundColor = Color.DarkGray,
                labelColor = Color.White,
                popUpLabel = { x, y ->
                    // Get the time label for this x position
                    val xIndex = x.toInt()
                    // Use allTimeLabels if provided (for complete time data), otherwise use xLabels
                    val labelsToUse = allTimeLabels ?: xLabels
                    val timeLabel = labelsToUse?.getOrNull(xIndex)?.takeIf { it.isNotEmpty() }
                        ?: x.toString()

                    // Format: "Time: 1:15AM\nValue: 75.32"
                    "Time: $timeLabel \nValue: ${String.format("%.2f", y)}"
                }
            )
        )

        // Optional invisible anchor line to enforce fixed Y range
        val lines = if (fixedYRange != null) {
            val lastX = points.lastOrNull()?.x ?: 1f
            val anchorLine = Line(
                dataPoints = listOf(
                    Point(0f, fixedYRange.start),
                    Point(lastX, fixedYRange.endInclusive)
                ),
                lineStyle = LineStyle(color = Color.Transparent),
                intersectionPoint = IntersectionPoint(color = Color.Transparent),
                selectionHighlightPoint = SelectionHighlightPoint(color = Color.Transparent),
                shadowUnderLine = ShadowUnderLine(color = Color.Transparent),
                selectionHighlightPopUp = SelectionHighlightPopUp(backgroundColor = Color.Transparent)
            )
            listOf(realLine, anchorLine)
        } else {
            listOf(realLine)
        }

        val chartData = LineChartData(
            linePlotData = LinePlotData(lines = lines),
            xAxisData = xAxis,
            yAxisData = yAxis,
        )

        LineChart(
            modifier = modifier,
            lineChartData = chartData
        )
    }
}
/*@Composable
fun MetricLineChart(
    title: String? = null,
    points: List<Point>,
    modifier: Modifier = Modifier,
    color: Color = Color.Cyan,
    fixedYRange: ClosedFloatingPointRange<Float>? = null,
    xLabels: List<String>? = null,
    ySteps: Int = 5,
    xSteps: Int = 24,
) {
    Column {
        if (!title.isNullOrBlank()) {
            Text(title, color = Color.White)
            Spacer(Modifier.height(8.dp))
        }

        if (points.isEmpty()) {
            Text("No data", color = Color.White.copy(alpha = 0.7f))
            return
        }

        // ----- Y range handling -----
        val (yMin, yMax) = if (fixedYRange != null) {
            fixedYRange.start to fixedYRange.endInclusive
        } else {
            val ys = points.map { it.y }
            var minY = ys.minOrNull() ?: 0f
            var maxY = ys.maxOrNull() ?: 0f
            // pad if flat so the line is visible
            if (abs(maxY - minY) < 1e-6f) {
                val pad = max(0.5f, abs(maxY) * 0.01f)
                minY -= pad
                maxY += pad
            }
            minY to maxY
        }

        val yAxis = AxisData.Builder()
            .steps(ySteps)
            .labelData { i: Int ->
                val v = yMin + (yMax - yMin) * (i / ySteps.toFloat())
                String.format("%.2f", v)
            }
            .build()

        val xAxis = AxisData.Builder()
            .steps((xLabels?.size ?: 1) - 1)
            .labelData { i ->
                xLabels?.getOrNull(i) ?: ""
            }
            .build()


        // Real visible line
        val realLine = Line(
            dataPoints = points,
            lineStyle = LineStyle(color = color),
            intersectionPoint = IntersectionPoint(radius = 4.dp, color = Black),
            selectionHighlightPopUp = SelectionHighlightPopUp(
                backgroundColor = Color.DarkGray,
                labelColor = Color.White
            )
        )


        // Optional invisible anchor line to enforce fixed Y range
        val lines = if (fixedYRange != null) {
            val lastX = points.lastOrNull()?.x ?: 1f
            val anchorLine = Line(
                dataPoints = listOf(
                    Point(0f, fixedYRange.start),
                    Point(lastX, fixedYRange.endInclusive)
                ),
                lineStyle = LineStyle(color = Color.Transparent),
                intersectionPoint = IntersectionPoint(color = Color.Transparent),
                selectionHighlightPoint = SelectionHighlightPoint(color = Color.Transparent),
                shadowUnderLine = ShadowUnderLine(color = Color.Transparent),
                selectionHighlightPopUp = SelectionHighlightPopUp(backgroundColor = Color.Transparent)
            )
            listOf(realLine, anchorLine)
        } else {
            listOf(realLine)
        }

        val chartData = LineChartData(
            linePlotData = LinePlotData(lines = lines),
            xAxisData = xAxis,
            yAxisData = yAxis,
        )

        LineChart(
            modifier = modifier,
            lineChartData = chartData
        )
    }
}*/