package com.example.airqualitytracker

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.model.Point
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PredictionChartViewModel : ViewModel() {

    // Current prediction points
    val tempPoints       = mutableStateListOf<Point>()
    val humidityPoints   = mutableStateListOf<Point>()
    val tvocPoints       = mutableStateListOf<Point>()
    val coPoints         = mutableStateListOf<Point>()
    val flammablePoints  = mutableStateListOf<Point>()
    val xLabels = mutableStateListOf<String>()
    val allTimeLabels = mutableStateListOf<String>() // All times for popup display

    // Previous prediction points for comparison
    val prevTempPoints       = mutableStateListOf<Point>()
    val prevHumidityPoints   = mutableStateListOf<Point>()
    val prevTvocPoints       = mutableStateListOf<Point>()
    val prevCoPoints         = mutableStateListOf<Point>()
    val prevFlammablePoints  = mutableStateListOf<Point>()
    val prevXLabels = mutableStateListOf<String>()
    val prevAllTimeLabels = mutableStateListOf<String>() // All times for popup display

    // Track if we have previous data to compare
    var hasPreviousData = mutableStateOf(false)
        private set

    // UI states
    var isLoading = mutableStateOf(false)
        private set
    var error = mutableStateOf<String?>(null)
        private set

    private val gson = Gson()

    fun fetchAndBuild(days: Float) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                // Save current data as previous before fetching new data
                if (tempPoints.isNotEmpty()) {
                    prevTempPoints.clear()
                    prevHumidityPoints.clear()
                    prevTvocPoints.clear()
                    prevCoPoints.clear()
                    prevFlammablePoints.clear()
                    prevXLabels.clear()
                    prevAllTimeLabels.clear()

                    prevTempPoints.addAll(tempPoints)
                    prevHumidityPoints.addAll(humidityPoints)
                    prevTvocPoints.addAll(tvocPoints)
                    prevCoPoints.addAll(coPoints)
                    prevFlammablePoints.addAll(flammablePoints)
                    prevXLabels.addAll(xLabels)
                    prevAllTimeLabels.addAll(allTimeLabels)

                    hasPreviousData.value = true
                }

                val el: JsonElement = Http.api.getPrediction(DaysRequest(days))
                val obj: JsonObject = if (el.isJsonArray) el.asJsonArray[1].asJsonObject else el.asJsonObject
                val resp: PredictionResponse = gson.fromJson(obj, PredictionResponse::class.java)
                Log.d("PredictionVM", el.toString())
                buildPoints(resp, days)
            } catch (e: Exception) {
                Log.e("PredictionVM", "fetch error", e)
                error.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearComparison() {
        prevTempPoints.clear()
        prevHumidityPoints.clear()
        prevTvocPoints.clear()
        prevCoPoints.clear()
        prevFlammablePoints.clear()
        prevXLabels.clear()
        prevAllTimeLabels.clear()
        hasPreviousData.value = false
    }

    private fun buildPoints(resp: PredictionResponse, days: Float) {
        tempPoints.clear()
        humidityPoints.clear()
        tvocPoints.clear()
        coPoints.clear()
        flammablePoints.clear()
        xLabels.clear()
        allTimeLabels.clear()

        // Round current time down to nearest 15-minute interval
        val now = LocalDateTime.now()
        val currentMinute = now.minute
        val roundedMinute = (currentMinute / 15) * 15 // Rounds down to 0, 15, 30, or 45
        val startTime = now.withMinute(roundedMinute).withSecond(0).withNano(0)

        val dateTimeFormatter = DateTimeFormatter.ofPattern("M/d h:mma") // e.g. "12/2 8:45PM"

        val predictiveCount = resp.forecasts.temperature?.size ?: 0
        val stepMinutes = resp.step_minutes ?: 15 // Default to 15 if not provided

        Log.d("PredictionVM", "Building points: predictiveCount=$predictiveCount, stepMinutes=$stepMinutes, days=$days")

        // Build x-axis labels - show every 4th label to prevent bunching
        for (i in 0 until predictiveCount) {
            val labelTime = startTime.plusMinutes((i * stepMinutes).toLong())

            // Add to allTimeLabels (for popup)
            allTimeLabels.add(labelTime.format(dateTimeFormatter))

            // Add to xLabels (for x-axis display - every 4th label)
            if (i % 4 == 0) {
                xLabels.add(labelTime.format(dateTimeFormatter))
            } else {
                xLabels.add("") // Empty string for hidden labels
            }
        }

        // Build data points with simple integer x-values
        resp.forecasts.temperature?.forEachIndexed { i, temp ->
            tempPoints.add(Point(i.toFloat(), temp * 9f / 5f + 32f))
        }
        resp.forecasts.humidity?.forEachIndexed { i, hum ->
            humidityPoints.add(Point(i.toFloat(), hum))
        }
        resp.forecasts.tvoc?.forEachIndexed { i, tvoc ->
            tvocPoints.add(Point(i.toFloat(), tvoc))
        }
        resp.forecasts.co?.forEachIndexed { i, co ->
            coPoints.add(Point(i.toFloat(), co))
        }
        resp.forecasts.flammable?.forEachIndexed { i, flam ->
            flammablePoints.add(Point(i.toFloat(), flam))
        }

        Log.d("PredictionVM", "Built ${tempPoints.size} forecast points")
    }
}
//If i want the data to start at Midnight
/*package com.example.airqualitytracker

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.model.Point
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PredictionChartViewModel : ViewModel() {

    // Current prediction points
    val tempPoints       = mutableStateListOf<Point>()
    val humidityPoints   = mutableStateListOf<Point>()
    val tvocPoints       = mutableStateListOf<Point>()
    val coPoints         = mutableStateListOf<Point>()
    val flammablePoints  = mutableStateListOf<Point>()
    val xLabels = mutableStateListOf<String>()
    val allTimeLabels = mutableStateListOf<String>() // All times for popup display

    // Previous prediction points for comparison
    val prevTempPoints       = mutableStateListOf<Point>()
    val prevHumidityPoints   = mutableStateListOf<Point>()
    val prevTvocPoints       = mutableStateListOf<Point>()
    val prevCoPoints         = mutableStateListOf<Point>()
    val prevFlammablePoints  = mutableStateListOf<Point>()
    val prevXLabels = mutableStateListOf<String>()
    val prevAllTimeLabels = mutableStateListOf<String>() // All times for popup display

    // Track if we have previous data to compare
    var hasPreviousData = mutableStateOf(false)
        private set

    // UI states
    var isLoading = mutableStateOf(false)
        private set
    var error = mutableStateOf<String?>(null)
        private set

    private val gson = Gson()

    fun fetchAndBuild(days: Float) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                // Save current data as previous before fetching new data
                if (tempPoints.isNotEmpty()) {
                    prevTempPoints.clear()
                    prevHumidityPoints.clear()
                    prevTvocPoints.clear()
                    prevCoPoints.clear()
                    prevFlammablePoints.clear()
                    prevXLabels.clear()
                    prevAllTimeLabels.clear()

                    prevTempPoints.addAll(tempPoints)
                    prevHumidityPoints.addAll(humidityPoints)
                    prevTvocPoints.addAll(tvocPoints)
                    prevCoPoints.addAll(coPoints)
                    prevFlammablePoints.addAll(flammablePoints)
                    prevXLabels.addAll(xLabels)
                    prevAllTimeLabels.addAll(allTimeLabels)

                    hasPreviousData.value = true
                }

                val el: JsonElement = Http.api.getPrediction(DaysRequest(days))
                val obj: JsonObject = if (el.isJsonArray) el.asJsonArray[1].asJsonObject else el.asJsonObject
                val resp: PredictionResponse = gson.fromJson(obj, PredictionResponse::class.java)
                Log.d("PredictionVM", el.toString())
                buildPoints(resp, days)
            } catch (e: Exception) {
                Log.e("PredictionVM", "fetch error", e)
                error.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearComparison() {
        prevTempPoints.clear()
        prevHumidityPoints.clear()
        prevTvocPoints.clear()
        prevCoPoints.clear()
        prevFlammablePoints.clear()
        prevXLabels.clear()
        prevAllTimeLabels.clear()
        hasPreviousData.value = false
    }

    private fun buildPoints(resp: PredictionResponse, days: Float) {
        tempPoints.clear()
        humidityPoints.clear()
        tvocPoints.clear()
        coPoints.clear()
        flammablePoints.clear()
        xLabels.clear()
        allTimeLabels.clear()

        // Start from midnight (12:00 AM) today
        val midnight = LocalDateTime.now().toLocalDate().atStartOfDay()

        val dateTimeFormatter = DateTimeFormatter.ofPattern("M/d h:mma") // e.g. "12/2 12:00AM"

        val predictiveCount = resp.forecasts.temperature?.size ?: 0
        val stepMinutes = resp.step_minutes ?: 15 // Default to 15 if not provided

        Log.d("PredictionVM", "Building points: predictiveCount=$predictiveCount, stepMinutes=$stepMinutes, days=$days")

        // Build x-axis labels - show every 4th label to prevent bunching
        for (i in 0 until predictiveCount) {
            val labelTime = midnight.plusMinutes((i * stepMinutes).toLong())

            // Add to allTimeLabels (for popup)
            allTimeLabels.add(labelTime.format(dateTimeFormatter))

            // Add to xLabels (for x-axis display - every 4th label)
            if (i % 4 == 0) {
                xLabels.add(labelTime.format(dateTimeFormatter))
            } else {
                xLabels.add("") // Empty string for hidden labels
            }
        }

        // Build data points with simple integer x-values
        resp.forecasts.temperature?.forEachIndexed { i, temp ->
            tempPoints.add(Point(i.toFloat(), temp * 9f / 5f + 32f))
        }
        resp.forecasts.humidity?.forEachIndexed { i, hum ->
            humidityPoints.add(Point(i.toFloat(), hum))
        }
        resp.forecasts.tvoc?.forEachIndexed { i, tvoc ->
            tvocPoints.add(Point(i.toFloat(), tvoc))
        }
        resp.forecasts.co?.forEachIndexed { i, co ->
            coPoints.add(Point(i.toFloat(), co))
        }
        resp.forecasts.flammable?.forEachIndexed { i, flam ->
            flammablePoints.add(Point(i.toFloat(), flam))
        }

        Log.d("PredictionVM", "Built ${tempPoints.size} forecast points")
    }
}*/