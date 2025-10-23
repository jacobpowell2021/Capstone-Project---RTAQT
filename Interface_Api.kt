package com.example.airqualitytracker

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import com.google.gson.JsonElement

data class DaysRequest(val days: Float)
data class PredictionResponse(
    val daysRequested: Float =0f,
    val forecastSteps: Int = 0,
    val intervalMinutes: Int = 0,
    val forecasts: Forecasts = Forecasts()
)

data class Forecasts(
    val temperature: List<Float> = emptyList(),
    val humidity: List<Float> = emptyList(),
    val flammable: List<Float> = emptyList(),
    val tvoc: List<Float> = emptyList(),
    val co: List<Float> = emptyList(),
    val stepMinutes: List<Int> = emptyList()
)
data class ChartResponse(
    val historical_last_48: List<SensorReading>,
    val predictive_last_96: List<SensorReading>
)
data class SensorReading(
    val Temperature: Float,
    val Humidity: Float,
    val FlammableGases: Float,
    val TVOC: Float,
    val CO: Float,
    val BatteryLife: Float,
    val EventProcessedUtcTime: String,
    val stepMinutes: List<Int> = emptyList()
)


interface PredictiveApi {
    @POST("api/http_trigger")
    suspend fun getPrediction(@Body request: DaysRequest): JsonElement

    @GET("api/data_pull_http_trigger")
    suspend fun getCurrentChartData(): JsonElement
}





