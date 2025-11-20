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

    // Previous prediction points for comparison
    val prevTempPoints       = mutableStateListOf<Point>()
    val prevHumidityPoints   = mutableStateListOf<Point>()
    val prevTvocPoints       = mutableStateListOf<Point>()
    val prevCoPoints         = mutableStateListOf<Point>()
    val prevFlammablePoints  = mutableStateListOf<Point>()
    val prevXLabels = mutableStateListOf<String>()

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

                    prevTempPoints.addAll(tempPoints)
                    prevHumidityPoints.addAll(humidityPoints)
                    prevTvocPoints.addAll(tvocPoints)
                    prevCoPoints.addAll(coPoints)
                    prevFlammablePoints.addAll(flammablePoints)
                    prevXLabels.addAll(xLabels)

                    hasPreviousData.value = true
                }

                val el: JsonElement = Http.api.getPrediction(DaysRequest(days))
                val obj: JsonObject = if (el.isJsonArray) el.asJsonArray[1].asJsonObject else el.asJsonObject
                val resp: PredictionResponse = gson.fromJson(obj, PredictionResponse::class.java)
                Log.d("PredictionVM", el.toString())
                buildPoints(resp)
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
        hasPreviousData.value = false
    }

    private fun buildPoints(resp: PredictionResponse) {
        tempPoints.clear()
        humidityPoints.clear()
        tvocPoints.clear()
        coPoints.clear()
        flammablePoints.clear()
        xLabels.clear()

        val now = LocalDateTime.now()
        val dateFormatter = DateTimeFormatter.ofPattern("M/d")
        val timeFormatter = DateTimeFormatter.ofPattern("h:mma")

        val predictiveCount = resp.forecasts.temperature?.size ?: 0
        val stepMinutes = resp.step_minutes ?: 5

        Log.d("PredictionVM", "Building points: predictiveCount=$predictiveCount, stepMinutes=$stepMinutes")

        val maxLabels = 8
        val labelInterval = maxOf(1, predictiveCount / maxLabels)

        for (i in 0 until predictiveCount) {
            if (i % labelInterval == 0 || i == predictiveCount - 1) {
                val labelTime = now.plusMinutes((i * stepMinutes).toLong())
                val date = labelTime.format(dateFormatter)
                val time = labelTime.format(timeFormatter)
                val label = "$date $time"
                xLabels.add(label)
            } else {
                xLabels.add("")
            }
        }

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



