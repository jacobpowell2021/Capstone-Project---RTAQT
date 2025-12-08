package com.example.airqualitytracker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class SensorMonitorWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val gson = Gson()

    override suspend fun doWork(): Result {
        return try {
            Log.d("SensorMonitor", "Background check started")

            // Fetch latest sensor data
            val el: JsonElement = Http.api.getCurrentChartData()
            val obj: JsonObject = if (el.isJsonArray) el.asJsonArray[1].asJsonObject else el.asJsonObject
            val resp: ChartResponse = gson.fromJson(obj, ChartResponse::class.java)

            // Get the most recent reading
            val latestReading = resp.historical_last_48.lastOrNull()

            if (latestReading != null) {
                // Check temperature
                val tempF = (latestReading.Temperature * 9f / 5f) + 32f
                when {
                    tempF < 60f -> {
                        showSystemNotification(
                            context,
                            "❄️ Low Temperature Alert",
                            "Temperature is ${String.format("%.1f", tempF)}°F - Below safe threshold!",
                            AlertType.TEMPERATURE_LOW
                        )
                        Log.d("SensorMonitor", "Low temp notification sent: $tempF°F")
                    }
                    tempF > 80f -> {
                        showSystemNotification(
                            context,
                            "🔥 High Temperature Alert",
                            "Temperature is ${String.format("%.1f", tempF)}°F - Above safe threshold!",
                            AlertType.TEMPERATURE_HIGH
                        )
                        Log.d("SensorMonitor", "High temp notification sent: $tempF°F")
                    }
                }

                // Check humidity
                val humidity = latestReading.Humidity
                when {
                    humidity > 60f -> {
                        showSystemNotification(
                            context,
                            "💧 High Humidity Alert",
                            "Humidity is ${String.format("%.1f", humidity)}% - Above recommended level!",
                            AlertType.HUMIDITY_HIGH
                        )
                        Log.d("SensorMonitor", "High humidity notification sent: $humidity%")
                    }
                    humidity < 20f -> {
                        showSystemNotification(
                            context,
                            "🏜️ Low Humidity Alert",
                            "Humidity is ${String.format("%.1f", humidity)}% - Below recommended level!",
                            AlertType.HUMIDITY_LOW
                        )
                        Log.d("SensorMonitor", "Low humidity notification sent: $humidity%")
                    }
                }

                // Check TVOC
                if (latestReading.TVOC > 500f) {
                    showSystemNotification(
                        context,
                        "🌫️ Air Quality Alert",
                        "Total VOC is ${String.format("%.1f", latestReading.TVOC)} ppb - Poor air quality detected!",
                        AlertType.TVOC_HIGH
                    )
                    Log.d("SensorMonitor", "TVOC notification sent: ${latestReading.TVOC} ppb")
                }

                // Check CO
                if (latestReading.CO > 9f) {
                    showSystemNotification(
                        context,
                        "☠️ Carbon Monoxide Alert",
                        "CO level is ${String.format("%.1f", latestReading.CO)} ppm - DANGEROUS levels detected!",
                        AlertType.CO_HIGH
                    )
                    Log.d("SensorMonitor", "CO notification sent: ${latestReading.CO} ppm")
                }

                // Check Flammable Gases
                if (latestReading.FlammableGases > 1000f) {
                    showSystemNotification(
                        context,
                        "💥 Flammable Gas Alert",
                        "Flammable gas level is ${String.format("%.1f", latestReading.FlammableGases)} ppm - High concentration detected!",
                        AlertType.FLAMMABLE_HIGH
                    )
                    Log.d("SensorMonitor", "Flammable gas notification sent: ${latestReading.FlammableGases} ppm")
                }

                // Check Battery
                val batteryPct = latestReading.BatteryLife.toInt()
                if (batteryPct < 20) {
                    showSystemNotification(
                        context,
                        "🔋 Low Battery Alert",
                        "Sensor battery is at $batteryPct% - Consider charging soon!",
                        AlertType.BATTERY_LOW
                    )
                    Log.d("SensorMonitor", "Low battery notification sent: $batteryPct%")
                }
            }

            Log.d("SensorMonitor", "Background check completed successfully")

            // Reschedule the next check (for testing mode)
            rescheduleWork()

            Result.success()
        } catch (e: Exception) {
            Log.e("SensorMonitor", "Background check failed", e)
            // Retry on failure
            Result.retry()
        }
    }

    private fun rescheduleWork() {
        // Get the interval from shared preferences or use default
        val sharedPrefs = context.getSharedPreferences("sensor_monitor_prefs", Context.MODE_PRIVATE)
        val intervalSeconds = sharedPrefs.getLong("check_interval_seconds", 30)

        // Reschedule for next run
        BackgroundMonitoringService.startMonitoring(context, intervalSeconds)
    }
}