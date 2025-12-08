package com.example.airqualitytracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Notification types with corresponding icons and colors
 */
enum class AlertType {
    TEMPERATURE_HIGH,
    TEMPERATURE_LOW,
    HUMIDITY_HIGH,
    HUMIDITY_LOW,
    TVOC_HIGH,
    CO_HIGH,
    FLAMMABLE_HIGH,
    BATTERY_LOW,
    GENERAL
}

/**
 * Enhanced notification system with custom icons for each alert type
 */
fun showSystemNotification(
    context: Context,
    title: String,
    message: String,
    alertType: AlertType = AlertType.GENERAL
) {
    val channelId = "air_quality_alerts"
    val notificationId = alertType.ordinal + 1000 // Unique ID per alert type

    // Create notification channel (required for Android 8.0+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val importance = when (alertType) {
            AlertType.CO_HIGH, AlertType.FLAMMABLE_HIGH -> NotificationManager.IMPORTANCE_HIGH
            else -> NotificationManager.IMPORTANCE_DEFAULT
        }

        val channel = NotificationChannel(
            channelId,
            "Air Quality Alerts",
            importance
        ).apply {
            description = "Notifications for concerning sensor readings"
            enableVibration(true)
            enableLights(true)
            lightColor = android.graphics.Color.RED
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Intent to open the app when notification is tapped
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    // Select icon and color based on alert type
    val (iconRes, color, priority) = getAlertStyle(alertType)

    // Build the notification
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(iconRes)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(priority)
        .setColor(color)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setVibrate(longArrayOf(0, 500, 250, 500)) // Vibration pattern
        .build()

    // Check for notification permission before showing
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires runtime permission check
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                with(NotificationManagerCompat.from(context)) {
                    notify(notificationId, notification)
                }
            } else {
                android.util.Log.w("NotificationHelper", "POST_NOTIFICATIONS permission not granted")
            }
        } else {
            // Below Android 13, no runtime permission needed
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, notification)
            }
        }
    } catch (e: SecurityException) {
        android.util.Log.e("NotificationHelper", "SecurityException when showing notification", e)
    }
}

/**
 * Get the appropriate icon, color, and priority for each alert type
 */
private fun getAlertStyle(alertType: AlertType): Triple<Int, Int, Int> {
    return when (alertType) {
        AlertType.TEMPERATURE_HIGH -> Triple(
            android.R.drawable.ic_dialog_alert, // 🔥 Fire/Alert icon
            android.graphics.Color.rgb(255, 87, 34), // Deep Orange
            NotificationCompat.PRIORITY_HIGH
        )

        AlertType.TEMPERATURE_LOW -> Triple(
            android.R.drawable.ic_dialog_info, // ❄️ Cold/Info icon
            android.graphics.Color.rgb(33, 150, 243), // Blue
            NotificationCompat.PRIORITY_DEFAULT
        )

        AlertType.HUMIDITY_HIGH -> Triple(
            android.R.drawable.ic_dialog_info, // 💧 Water drop icon
            android.graphics.Color.rgb(3, 169, 244), // Light Blue
            NotificationCompat.PRIORITY_DEFAULT
        )

        AlertType.HUMIDITY_LOW -> Triple(
            android.R.drawable.ic_dialog_info, // Dry air icon
            android.graphics.Color.rgb(255, 193, 7), // Amber
            NotificationCompat.PRIORITY_DEFAULT
        )

        AlertType.TVOC_HIGH -> Triple(
            android.R.drawable.ic_dialog_alert, // 🌫️ Air quality icon
            android.graphics.Color.rgb(156, 39, 176), // Purple
            NotificationCompat.PRIORITY_HIGH
        )

        AlertType.CO_HIGH -> Triple(
            android.R.drawable.ic_dialog_alert, // ☠️ Danger icon
            android.graphics.Color.rgb(244, 67, 54), // Red - CRITICAL
            NotificationCompat.PRIORITY_MAX
        )

        AlertType.FLAMMABLE_HIGH -> Triple(
            android.R.drawable.ic_dialog_alert, // 💥 Fire/Explosion icon
            android.graphics.Color.rgb(255, 152, 0), // Orange
            NotificationCompat.PRIORITY_MAX
        )

        AlertType.BATTERY_LOW -> Triple(
            android.R.drawable.ic_dialog_info, // 🔋 Battery icon
            android.graphics.Color.rgb(158, 158, 158), // Gray
            NotificationCompat.PRIORITY_LOW
        )

        AlertType.GENERAL -> Triple(
            android.R.drawable.ic_dialog_info, // ℹ️ Info icon
            android.graphics.Color.rgb(96, 125, 139), // Blue Gray
            NotificationCompat.PRIORITY_DEFAULT
        )
    }
}

