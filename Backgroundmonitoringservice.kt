package com.example.airqualitytracker

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

object BackgroundMonitoringService {

    private const val WORK_NAME = "sensor_monitoring_work"
    private const val TAG = "BackgroundMonitoring"

    /**
     * Start background monitoring with specified interval
     * @param context Application context
     * @param intervalSeconds How often to check (default: 30 seconds) - FOR TESTING ONLY
     */
    fun startMonitoring(context: Context, intervalSeconds: Long = 30) {
        Log.d(TAG, "Starting background monitoring with $intervalSeconds second interval")

        // Save interval to SharedPreferences for rescheduling
        val sharedPrefs = context.getSharedPreferences("sensor_monitor_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putLong("check_interval_seconds", intervalSeconds).apply()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Requires internet
            .build()

        // For testing: Use OneTimeWorkRequest in a loop instead of PeriodicWorkRequest
        // PeriodicWorkRequest has a minimum of 15 minutes
        val monitoringRequest = OneTimeWorkRequestBuilder<SensorMonitorWorker>()
            .setConstraints(constraints)
            .setInitialDelay(intervalSeconds, TimeUnit.SECONDS)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            monitoringRequest
        )

        Log.d(TAG, "Background monitoring scheduled successfully")
    }

    /**
     * Stop background monitoring
     */
    fun stopMonitoring(context: Context) {
        Log.d(TAG, "Stopping background monitoring")
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Check if monitoring is currently active
     */
    fun isMonitoringActive(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WORK_NAME)
            .get()

        return workInfos.any {
            it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
        }
    }

    /**
     * Trigger an immediate check (in addition to periodic checks)
     */
    fun triggerImmediateCheck(context: Context) {
        Log.d(TAG, "Triggering immediate sensor check")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateRequest = OneTimeWorkRequestBuilder<SensorMonitorWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(immediateRequest)
    }
}