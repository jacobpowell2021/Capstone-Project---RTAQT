package com.example.airqualitytracker

import android.hardware.Sensor
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import androidx.compose.runtime.mutableStateOf
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

/* private fun buildLatestPoints (resp: ChartResponse){
     tempPoints.clear()
     humidityPoints.clear()
     tvocPoints.clear()
     coPoints.clear()
     flammablePoints.clear()
     xLabels.clear()
     Log.d("LatestVM", "$resp.temperature")
     resp.historical_last_48.forEachIndexed { i, reading ->
         tempPoints.add(Point(i.toFloat() / 12f, reading.temperature))
     }
     /*
             resp.humidity?.forEachIndexed{i, v ->
                 humidityPoints.add(Point(i.toFloat()/12f, v))
             }
             resp.tvoc?.forEachIndexed{i, v ->
                 tvocPoints.add(Point(i.toFloat()/12f, v))
             }
             resp.flammable?.forEachIndexed{i, v ->
                 flammablePoints.add(Point(i.toFloat()/12f, v))
             }
             resp.co?.forEachIndexed{i, v ->
                 coPoints.add(Point(i.toFloat()/12f, v))
             }
             resp.stepMinutes?.forEach { m ->
                 xLabels.add("${m}m")
             }
             // Labels like "0m", "60m", "180m"…
             resp.stepMinutes.forEach { m -> xLabels.add("${m}m") }*/
 }
}*/


//all of this listens to MQTT which might now be needed anymore
/*class ChartViewModel : ViewModel() {

    // Persisted series (survive navigation)
    val humidityPoints       = mutableStateListOf<Point>()   // HUMIDITY points
    val humidityLabels       = mutableStateListOf<String>()  // HUMIDITY labels

    val temperaturePoints = mutableStateListOf<Point>()  // TEMPERATURE points
    val temperatureLabels = mutableStateListOf<String>() // TEMPERATURE labels

    val particlePoints    = mutableStateListOf<Point>()  // PARTICLE points
    val particleLabels    = mutableStateListOf<String>() // PARTICLE labels

    // Control flags
    var started = false
        private set
    private var lastPlottedHumidity: String? = null
    private var lastPlottedTemperature: String? = null
    private var lastPlottedParticle: String? = null

    fun markStarted() { started = true }

    /** HUMIDITY: Add a point if the incoming string is a numeric value */
    fun appendHumidityIfNew(raw: String, timestamp: String) {
        //if (raw == lastPlottedHumidity) return
        val v = raw.toFloatOrNull() ?: return
        //lastPlottedHumidity = raw

        val idx = humidityPoints.size.toFloat()
        humidityPoints.add(Point(idx, v))
        Log.d("Chart List Humidity", "The list is currently $humidityPoints")
        humidityLabels.add(timestamp)

        //trim(humidityPoints, humidityLabels)
    }

    /** TEMPERATURE: Add a point if the incoming string is a new numeric value */
    fun appendTemperatureIfNew(raw: String, timestamp: String) {
        //might be messing up the point allocation because of duplicates. if (raw == lastPlottedTemperature) return//returns if the value is the same
        val v = raw.toFloatOrNull() ?: return    //turn the value to a float
        lastPlottedTemperature = raw             //sets a new plotted temperature

        val idx = temperaturePoints.size.toFloat() //gets the size of the list for plotting the next value
        temperaturePoints.add(Point(idx, v))
        Log.d("Chart List Temperature", "The list is currently $temperaturePoints")
        temperatureLabels.add(timestamp)

        //trim(temperaturePoints, temperatureLabels)
    }

    /** PARTICLE: Add a point if the incoming string is a new numeric value */
    // In your ViewModel (or wherever these live)
    private var particleParseAttempts = 0
    private var particleParseFailures = 0
    //private var particleDuplicateSkips = 0
// private var particleXCounter = 0f // (optional) if you don't want x to shift when trimming

    fun appendParticleIfNew(raw: String, timestamp: String) {
        particleParseAttempts++

        // Try parse
        val v = raw.toFloatOrNull()
        if (v == null) {
            particleParseFailures++
            Log.w("Chartfloat", "Unsuccessful conversion for '$raw' at $timestamp")//trying to figure out what is mismatching the points on the chart
            return
        }

        Log.d("Chartfloat", "Successful conversion: $v")

        /*// Optional: skip duplicates (compare strings or floats with tolerance). I do not want to skip duplicates
        if (raw == lastPlottedParticle) {
            particleDuplicateSkips++
            Log.d("Chartfloat", "Duplicate value, skipping: $raw")
            return
        }*/
        lastPlottedParticle = raw
        val idx = particlePoints.size.toFloat()
        particlePoints.add(Point(idx, v))
        Log.d("Chart List Particle", "The list is currently $particlePoints")
        particleLabels.add(timestamp)

        // Keep lists bounded and in sync
        //trim(particlePoints, particleLabels)
    }


    /** Clear all series */
    fun clearSeries() {
        humidityPoints.clear();        humidityLabels.clear();        lastPlottedHumidity = null
        temperaturePoints.clear(); temperatureLabels.clear(); lastPlottedTemperature = null
        particlePoints.clear();    particleLabels.clear();    lastPlottedParticle = null
    }

    // ---- helpers ----
    /**
     * Enforce max length by removing oldest items from BOTH lists together.
     * Assumes points and labels are 1:1 (same order, same length).
     */
    /*private fun trim(points: MutableList<Point>, labels: MutableList<String>, max: Int = 600) {
        // Optional sanity check to catch drift during development
        require(points.size == labels.size) { "points/labels out of sync: ${points.size} vs ${labels.size}" }

        // Remove as many as needed (not just one)
        while (points.size > max) {
            points.removeAt(0)   // O(n)
            labels.removeAt(0)   // O(n)
        }
    }*/


}
*/

/*package com.example.airqualitytracker

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import co.yml.charts.common.model.Point

class ChartViewModel : ViewModel() {
    // MQTT-exposed states (strings from your MqttManager)
    val temperatureState = mutableStateOf("...")
    val humidityState    = mutableStateOf("...")
    val particleState    = mutableStateOf("...")

    // Persisted series (survive navigation)
    val pointsData = mutableStateListOf<Point>()
    val dateLabels = mutableStateListOf<String>()

    // Control flags
    var started = false
        private set
    private var lastPlottedHumidity: String? = null

    fun markStarted() { started = true }

    /** Add a point if the incoming string is a new numeric value */
    fun appendHumidityIfNew(raw: String, timestamp: String) {
        if (raw == lastPlottedHumidity) return        // dedupe on re-entry
        val v = raw.toFloatOrNull() ?: return
        lastPlottedHumidity = raw

        val idx = pointsData.size.toFloat()
        pointsData.add(Point(idx, v))
        dateLabels.add(timestamp)

        // Trim history if desired
        val maxPoints = 300
        if (pointsData.size > maxPoints) pointsData.removeAt(0)
        if (dateLabels.size > maxPoints) dateLabels.removeAt(0)
    }

    fun clearSeries() {
        pointsData.clear()
        dateLabels.clear()
        lastPlottedHumidity = null
    }

}
*/