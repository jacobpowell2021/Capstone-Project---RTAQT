package com.example.airqualitytracker

import android.util.Log

object TESTING {

    fun runTest() {
        val sampleJson = """
            {
              "body": {
                "Temperature": 23.5,
                "Humidity": 55,
                "FlammableGases": 120,
                "TVOC": 0.45,
                "CO": 3.2
              },
              "enqueuedTime": "Tue Sep 09 2025 13:02:14 GMT-0500 (Central Daylight Time)"
            }
        """.trimIndent()

        val parsed = TelemetryParser.parse(sampleJson)

        if (parsed != null) {
            val b = parsed.body
            val ts = TelemetryParser.parseEnqueuedInstant(parsed.enqueuedTime)

            Log.d("TESTING", "Temperature = ${b.Temperature}")
            Log.d("TESTING", "Humidity    = ${b.Humidity}")
            Log.d("TESTING", "Flammable   = ${b.FlammableGases}")
            Log.d("TESTING", "TVOC        = ${b.TVOC}")
            Log.d("TESTING", "CO          = ${b.CO}")
            Log.d("TESTING", "Timestamp   = $ts")
        } else {
            Log.e("TESTING", "Parser failed!")
        }
    }
}
