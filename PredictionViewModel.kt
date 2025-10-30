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

    // Chart points per series
    val tempPoints       = mutableStateListOf<Point>()
    val humidityPoints   = mutableStateListOf<Point>()
    val tvocPoints       = mutableStateListOf<Point>()
    val coPoints         = mutableStateListOf<Point>()
    val flammablePoints  = mutableStateListOf<Point>()

    // X-axis labels derived from step_minutes
    val xLabels = mutableStateListOf<String>()

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
                val el: JsonElement = Http.api.getPrediction(DaysRequest(days))
                val obj: JsonObject = if (el.isJsonArray) el.asJsonArray[1].asJsonObject else el.asJsonObject
                val resp: PredictionResponse = gson.fromJson(obj, PredictionResponse::class.java)
                buildPoints(resp)
                buildLast96Hours(xLabels)
            } catch (e: Exception) {
                Log.e("PredictionVM", "fetch error", e)
                error.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun buildPoints(resp: PredictionResponse) {
        tempPoints.clear()
        humidityPoints.clear()
        tvocPoints.clear()
        coPoints.clear()
        flammablePoints.clear()

        Log.d("PredictionVM", "${resp.forecasts.temperature}")
        resp.forecasts.temperature?.forEachIndexed { i, v ->
            tempPoints.add(Point(i.toFloat()/2f, v*9/5 +32))
        }
        resp.forecasts.humidity?.forEachIndexed { i, v ->
            humidityPoints.add(Point(i.toFloat()/2f, v))
        }
        resp.forecasts.tvoc?.forEachIndexed { i, v ->
            tvocPoints.add(Point(i.toFloat()/2f, v))
        }
        resp.forecasts.co?.forEachIndexed { i, v ->
            coPoints.add(Point(i.toFloat()/2f, v))
        }
        resp.forecasts.flammable?.forEachIndexed { i, v ->
            flammablePoints.add(Point(i.toFloat()/2f, v))
        }
    }
}

fun buildLast96Hours(xLabels: MutableList<String>) {
    xLabels.clear()

    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("h:mma") // e.g. 5:56PM

    // Go back 11 hours so you have 12 total (current + previous 11)
    for (i in 95 downTo 0) {
        val labelTime = now.minusHours(i.toLong())
        xLabels.add(labelTime.format(formatter))
    }
}

//VIEWMODEL that works
/*data class PredictionResponse(
    val days_requested: Float,
    val forecast_steps: Int,
    val interval_minutes: Int,
    val forecasts: Forecasts
)
data class Forecasts(
    val temperature: List<Float>,
    val humidity: List<Float>,
    val flammable: List<Float>,
    val tvoc: List<Float>,
    val co: List<Float>,
    val step_minutes: List<Int>
)

class PredictionViewModel : ViewModel() {
    var temperature = mutableStateOf<List<Float>>(emptyList()); private set
    var humidity    = mutableStateOf<List<Float>>(emptyList()); private set
    var flammable   = mutableStateOf<List<Float>>(emptyList()); private set
    var tvoc        = mutableStateOf<List<Float>>(emptyList()); private set
    var co          = mutableStateOf<List<Float>>(emptyList()); private set

    private val gson = Gson()

    // Chart points per series
    val tempPoints       = mutableStateListOf<Point>()
    val humidityPoints   = mutableStateListOf<Point>()
    val tvocPoints       = mutableStateListOf<Point>()
    val coPoints         = mutableStateListOf<Point>()
    val flammablePoints  = mutableStateListOf<Point>()


    fun fetchPrediction(days: Float) {
        viewModelScope.launch {
            try {
                val el: JsonElement = Http.api.getPrediction(DaysRequest(days))

                // Unwrap if the response is [200, {...}]
                val obj: JsonObject = if (el.isJsonArray) {
                    el.asJsonArray.get(1).asJsonObject
                } else {
                    el.asJsonObject
                }

                val resp = gson.fromJson(obj, PredictionResponse::class.java)
                temperature.value = resp.forecasts.temperature
                humidity.value    = resp.forecasts.humidity
                flammable.value   = resp.forecasts.flammable
                tvoc.value        = resp.forecasts.tvoc
                co.value          = resp.forecasts.co

            } catch (e: Exception) {
                // handle error state for your UI
            }
        }
    }
}
*/



/*class PredictionViewModel : ViewModel() {
    var predictionResult = mutableStateOf("")
        private set

    fun fetchPrediction(days: Float) {
        viewModelScope.launch {
            try {
                Log.d("PredictionViewModel", "Sending days=$days")
                val resp = Http.api.getPrediction(DaysRequest(days))
                Log.d("PredictionViewModel", predictionResult.value)
                predictionResult.value = resp.prediction.toString()
                Log.d("PredictionViewModel", predictionResult.value)
            } catch (e: Exception) {
                predictionResult.value = "Error: ${e.message}"
            }
        }
    }
}*/


