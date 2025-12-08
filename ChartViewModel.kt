package com.example.airqualitytracker

import android.hardware.Sensor
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.model.Point
import com.example.airqualitytracker.DaysRequest
import com.example.airqualitytracker.Http
import kotlinx.coroutines.launch
import com.example.airqualitytracker.PredictionResponse
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay



class LatestChartViewModel : ViewModel() {

    // Chart points per series (Historical - Last 48 hours)
    val tempPoints = mutableStateListOf<Point>()
    val humidityPoints = mutableStateListOf<Point>()
    val tvocPoints = mutableStateListOf<Point>()
    val coPoints = mutableStateListOf<Point>()
    val flammablePoints = mutableStateListOf<Point>()
    val batteryChargePoints = mutableStateListOf<Int>()
    val time = mutableStateListOf<String>()

    // Predictive chart points (Next 96 hours forecast)
    val predictiveTempPoints = mutableStateListOf<Point>()
    val predictiveHumidityPoints = mutableStateListOf<Point>()
    val predictiveTvocPoints = mutableStateListOf<Point>()
    val predictiveCoPoints = mutableStateListOf<Point>()
    val predictiveFlammablePoints = mutableStateListOf<Point>()

    var latestBattery by mutableStateOf<Int?>(null)
        private set
    // X-axis labels derived from step_minutes
    val xLabels = mutableStateListOf<String>()
    val historicalAllTimeLabels = mutableStateListOf<String>() // All times for popup display
    val predictiveXLabels = mutableStateListOf<String>()
    val predictiveAllTimeLabels = mutableStateListOf<String>() // All times for popup display

    var isLoading by mutableStateOf(true)
        private set

    private val gson = Gson()

    // --- Jobs so we can control which loop runs ---
    private var chartJob: Job? = null
    private var latestOnlyJob: Job? = null

    // Stop helpers (optional but nice to have)
    fun stopAutoFetchCharts() {
        chartJob?.cancel()
        chartJob = null
    }
    fun stopAutoFetchLatestOnly() {
        latestOnlyJob?.cancel()
        latestOnlyJob = null
    }


    fun fetch() {
        viewModelScope.launch {
            try {
                val el: JsonElement = Http.api.getCurrentChartData()
                val obj: JsonObject =
                    if (el.isJsonArray) el.asJsonArray[1].asJsonObject else el.asJsonObject
                val resp: ChartResponse = gson.fromJson(obj, ChartResponse::class.java)
                Log.d("LatestVM", el.toString())
                buildLast47Hours(xLabels)
                buildLast47HoursAllLabels(historicalAllTimeLabels) // Build all labels for popups
                buildNext96Hours(predictiveXLabels)
                buildNext96HoursAllLabels(predictiveAllTimeLabels) // Build all labels for popups
                Log.d("LatestVMLabels", xLabels.toString())
                Log.d("LatestVMPredictiveLabels", predictiveXLabels.toString())
                buildLatestPoints(resp)
                buildPredictivePoints(resp)
                updateLatestFromResponse(resp)
            } catch (e: Exception) {
                Log.e("LatestVM", "fetch error", e)
            } finally {
                isLoading = false  // stop loading once complete
            }
        }
    }


    private fun buildLatestPoints(resp: ChartResponse) {
        tempPoints.clear()
        humidityPoints.clear()
        tvocPoints.clear()
        coPoints.clear()
        flammablePoints.clear()
        batteryChargePoints.clear()

        Log.d("LatestVM", "Building latest points...")

        resp.historical_last_48.forEachIndexed { i, reading ->
            // X-axis spacing: each point is 15 minutes apart (i = 0, 1, 2, 3...)
            val x = i.toFloat()

            var y = (reading.Temperature * 9/5) + 32
            // Add point
            tempPoints.add(Point(x, y))

            y = reading.Humidity
            humidityPoints.add(Point(x,y)) //adds humidity point

            y = reading.FlammableGases
            flammablePoints.add(Point(x,y))

            y = reading.TVOC
            tvocPoints.add(Point(x,y))

            y = reading.CO
            coPoints.add(Point(x,y))

            batteryChargePoints.add(reading.BatteryLife.toInt())
            time.add(reading.EventProcessedUtcTime)
            // --- Format timestamp to hh:mm ---
            val label = try {
                val parsedUtc = OffsetDateTime.parse(reading.EventProcessedUtcTime)
                val localTime = parsedUtc.atZoneSameInstant(ZoneId.systemDefault())
                localTime.format(DateTimeFormatter.ofPattern("hh:mm", Locale.US))
            } catch (e: Exception) {
                Log.e("LatestVM", "Time parse error", e)
                "--:--"
            }
            // Log each addition
            Log.d(
                "LatestVM",
                "Added tempPoint: x=$x, y=$y | Total points so far: ${tempPoints.size}"
            )

            // Optional: also log current list contents if you want full trace
            Log.d("LatestVM", "Current tempPoints list: $tempPoints")
        }
        latestBattery = batteryChargePoints.lastOrNull()

        Log.d("LatestVM", "Finished building points. Total=${tempPoints.size}")
    }

    private fun buildPredictivePoints(resp: ChartResponse) {
        predictiveTempPoints.clear()
        predictiveHumidityPoints.clear()
        predictiveTvocPoints.clear()
        predictiveCoPoints.clear()
        predictiveFlammablePoints.clear()

        Log.d("LatestVM", "Building predictive points...")

        resp.predictive_last_96.forEachIndexed { i, reading ->
            // X-axis spacing: each point is 15 minutes apart (i = 0, 1, 2, 3...)
            val x = i.toFloat()

            // Temperature: Convert Celsius to Fahrenheit
            var y = (reading.Temperature * 9/5) + 32
            predictiveTempPoints.add(Point(x, y))

            // Humidity
            y = reading.Humidity
            predictiveHumidityPoints.add(Point(x, y))

            // Flammable Gases
            y = reading.FlammableGases
            predictiveFlammablePoints.add(Point(x, y))

            // TVOC
            y = reading.TVOC
            predictiveTvocPoints.add(Point(x, y))

            // CO
            y = reading.CO
            predictiveCoPoints.add(Point(x, y))

            // Log each addition
            Log.d(
                "LatestVM",
                "Added predictive tempPoint: x=$x, y=$y | Total predictive points: ${predictiveTempPoints.size}"
            )
        }

        Log.d("LatestVM", "Finished building predictive points. Total=${predictiveTempPoints.size}")
    }

    // REMOVED: Auto-refresh from init block
    // The HomeScreen will now control when to refresh via its LaunchedEffect
    init {
        Log.d("LatestVM", "ViewModel initialized - no auto-refresh started")
        // You can add an initial fetch here if needed:
        // fetch()
    }

    private fun startAutoFetchCharts(intervalMs: Long = 6_000L) {
        chartJob?.cancel()
        chartJob = viewModelScope.launch {
            while (isActive) {
                fetch()                 // <-- does charts + labels + snapshot
                delay(intervalMs)
            }
        }
    }

    private fun startAutoFetchLatestOnly(intervalMs: Long = 30_000L) {
        latestOnlyJob?.cancel()
        latestOnlyJob = viewModelScope.launch {
            while (isActive) {
                fetchLatestOnly()       // <-- ONLY updates `latestSnapshot` (no charts)
                delay(intervalMs)
            }
        }
    }


    private fun startAutoFetchLatest() {
        viewModelScope.launch {
            while (true) {
                fetchLatestOnly()
                kotlinx.coroutines.delay(6_000L) // 15 seconds
            }
        }
    }

    // 1) A simple container for the latest values (already in the ViewModel file)
    data class LatestSnapshot(
        val temperatureF: Float,
        val humidity: Float,
        val tvoc: Float,
        val co: Float,
        val flammable: Float,
        val batteryPct: Int,
        val eventLocalTime: String,     // formatted & safe (e.g., "05:12 PM" or "--:--")
    )


    // 2) Hold the latest snapshot as observable state
    var latestSnapshot by mutableStateOf<LatestSnapshot?>(null)
        private set

    // 3) Create/update snapshot from a single reading
    private fun buildSnapshotFrom(
        tempC: Float?,
        humidity: Float?,
        tvoc: Float?,
        co: Float?,
        flammable: Float?,
        batteryPct: Int?,
        eventProcessedUtcTime: String?
    ): LatestSnapshot {
        val tempF = ((tempC ?: Float.NaN) * 9f / 5f) + 32f
        val nowFormatted = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm:ss a", java.util.Locale.US))

        return LatestSnapshot(
            temperatureF = tempF,
            humidity = humidity ?: Float.NaN,
            tvoc = tvoc ?: Float.NaN,
            co = co ?: Float.NaN,
            flammable = flammable ?: Float.NaN,
            batteryPct = batteryPct ?: -1,
            eventLocalTime = nowFormatted   // ⬅ use current time

        )
    }


    // 4) Extract only the most recent reading from the HTTP response and update state
    private fun updateLatestFromResponse(resp: ChartResponse) {
        val r = resp.historical_last_48.lastOrNull() ?: run {
            Log.w("LatestVM", "No readings available in response.")
            latestSnapshot = null
            latestBattery = null
            return
        }

        // Update the single snapshot
        latestSnapshot = buildSnapshotFrom(
            tempC = r.Temperature,
            humidity = r.Humidity,
            tvoc = r.TVOC,
            co = r.CO,
            flammable = r.FlammableGases,
            batteryPct = r.BatteryLife.toInt(),
            eventProcessedUtcTime = r.EventProcessedUtcTime
        )

        // Keep your existing single-value for battery as well (if you still want it)
        latestBattery = latestSnapshot?.batteryPct
    }

    // 5) Optional: a lightweight fetch that ONLY updates latest values (no chart work)
    fun fetchLatestOnly() {
        viewModelScope.launch {
            try {
                Log.d("LatestVM", "fetchLatestOnly() called")
                val el: JsonElement = Http.api.getCurrentChartData()
                val obj: JsonObject = if (el.isJsonArray) el.asJsonArray[1].asJsonObject else el.asJsonObject
                val resp: ChartResponse = gson.fromJson(obj, ChartResponse::class.java)
                updateLatestFromResponse(resp)
            } catch (e: Exception) {
                Log.e("LatestVM", "fetchLatestOnly error", e)
            }
        }
    }
}

fun buildLast47Hours(xLabels: MutableList<String>) {
    xLabels.clear()

    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("h:mma") // e.g. 8:45PM

    // Round current time down to nearest 15-minute interval
    val currentMinute = now.minute
    val roundedMinute = (currentMinute / 15) * 15 // Rounds down to 0, 15, 30, or 45
    val roundedNow = now.withMinute(roundedMinute).withSecond(0).withNano(0)

    // Generate 48 labels going back 12 hours (48 * 15 minutes = 720 minutes = 12 hours)
    // Show only every 4th label (every hour) to prevent bunching
    for (i in 47 downTo 0) {
        val labelTime = roundedNow.minusMinutes((i * 15).toLong())
        if (i % 4 == 0) {
            xLabels.add(labelTime.format(formatter))
        } else {
            xLabels.add("") // Empty string for hidden labels
        }
    }
}

fun buildLast47HoursAllLabels(allLabels: MutableList<String>) {
    allLabels.clear()

    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("h:mma") // e.g. 8:45PM

    // Round current time down to nearest 15-minute interval
    val currentMinute = now.minute
    val roundedMinute = (currentMinute / 15) * 15 // Rounds down to 0, 15, 30, or 45
    val roundedNow = now.withMinute(roundedMinute).withSecond(0).withNano(0)

    // Generate ALL 48 labels going back 12 hours for popup display
    for (i in 47 downTo 0) {
        val labelTime = roundedNow.minusMinutes((i * 15).toLong())
        allLabels.add(labelTime.format(formatter))
    }
}

fun buildNext96Hours(xLabels: MutableList<String>) {
    xLabels.clear()

    val midnight = LocalDateTime.now().toLocalDate().atStartOfDay() // Start at 12:00 AM today
    val formatter = DateTimeFormatter.ofPattern("h:mma") // e.g. 12:00AM

    // Generate labels for 96 fifteen-minute intervals (24 hours)
    // Show only every 4th label (every hour) to prevent bunching
    for (i in 0 until 96) {
        if (i % 4 == 0) {
            val labelTime = midnight.plusMinutes((i * 15).toLong()) // Each interval is 15 minutes
            xLabels.add(labelTime.format(formatter))
        } else {
            xLabels.add("") // Empty string for hidden labels
        }
    }
}

fun buildNext96HoursAllLabels(allLabels: MutableList<String>) {
    allLabels.clear()

    val midnight = LocalDateTime.now().toLocalDate().atStartOfDay() // Start at 12:00 AM today
    val formatter = DateTimeFormatter.ofPattern("h:mma") // e.g. 12:15AM

    // Generate ALL labels for 96 fifteen-minute intervals (24 hours)
    // This is used for popup display, not x-axis
    for (i in 0 until 96) {
        val labelTime = midnight.plusMinutes((i * 15).toLong()) // Each interval is 15 minutes
        allLabels.add(labelTime.format(formatter))
    }
}