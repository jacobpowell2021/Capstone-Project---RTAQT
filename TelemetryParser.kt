package com.example.airqualitytracker

import android.util.Log
import com.google.gson.Gson
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Data models
data class TelemetryEnvelope(
    val body: TelemetryBody,
    val enqueuedTime: String
)

data class TelemetryBody(
    val Temperature: Float,
    val Humidity: Float,
    val FlammableGases: Float,
    val TVOC: Float,
    val CO: Float
)

object TelemetryParser {
    private val gson = Gson()
    private val timeFmt = DateTimeFormatter.ofPattern(
        "EEE MMM dd yyyy HH:mm:ss 'GMT'Z",
        Locale.US
    )

    /**
     * Parse JSON string from MQTT into [TelemetryEnvelope].
     * Returns null if parsing fails.
     */
    fun parse(json: String): TelemetryEnvelope? = try {
        gson.fromJson(json, TelemetryEnvelope::class.java)
    } catch (e: Exception) {
        Log.e("TelemetryParser", "Failed to parse: ${e.message}")
        null
    }

    /**
     * Parse the enqueuedTime field into an Instant (UTC).
     */
    fun parseEnqueuedInstant(enqueued: String) = try {
        val cleaned = enqueued.substringBefore(" (").trim()
        ZonedDateTime.parse(cleaned, timeFmt).toInstant()
    } catch (e: Exception) {
        Log.e("TelemetryParser", "Failed to parse enqueuedTime: ${e.message}")
        null
    }
}
