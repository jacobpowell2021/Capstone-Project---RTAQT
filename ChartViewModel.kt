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


class LatestChartViewModel : ViewModel() {
    // Chart points per series

    val tempPoints = mutableStateListOf<Point>()
    val humidityPoints = mutableStateListOf<Point>()
    val tvocPoints = mutableStateListOf<Point>()
    val coPoints = mutableStateListOf<Point>()
    val flammablePoints = mutableStateListOf<Point>()
    val batteryChargePoints = mutableStateListOf<Int>()
    val time = mutableStateListOf<String>()

    var latestBattery by mutableStateOf<Int?>(null)
        private set
    // X-axis labels derived from step_minutes
    val xLabels = mutableStateListOf<String>()
    var isLoading by mutableStateOf(true)
        private set

    private val gson = Gson()


    fun fetch() {
        viewModelScope.launch {
            try {
                val el: JsonElement = Http.api.getCurrentChartData()
                val obj: JsonObject =
                    if (el.isJsonArray) el.asJsonArray[1].asJsonObject else el.asJsonObject
                val resp: ChartResponse = gson.fromJson(obj, ChartResponse::class.java)
                Log.d("LatestVM", el.toString())
                buildLast47Hours(xLabels)
                Log.d("LatestVMLabels", xLabels.toString())
                buildLatestPoints(resp)
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
            val x = i.toFloat() / 2f

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
    init {
        startAutoFetch()
    }

    private fun startAutoFetch() {
        viewModelScope.launch {
            while (true) {
                fetch()
                kotlinx.coroutines.delay(6_000L) // 15 seconds
            }
        }
    }
}
fun getLatestTime(): Pair<String, String> {
    val now = LocalDateTime.now()
    val currentZone = now.atZone(ZoneId.systemDefault())

    val timeOnly = currentZone.format(DateTimeFormatter.ofPattern("hh:mm", Locale.US))
    val ampm = currentZone.format(DateTimeFormatter.ofPattern("a", Locale.US))

    return Pair(timeOnly, ampm)
}


fun buildLast47Hours(xLabels: MutableList<String>) {
    xLabels.clear()

    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("h:mma") // e.g. 5:56PM

    // Go back 11 hours so you have 12 total (current + previous 11)
    for (i in 23 downTo 0) {
        val labelTime = now.minusHours(i.toLong())
        xLabels.add(labelTime.format(formatter))
    }
}
