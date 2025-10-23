package com.example.airqualitytracker

//added this the push notification to happen outside of the home screen
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.airqualitytracker.R

fun showSystemNotification(context: Context, title: String, message: String) {
    val channelId = "sensor_alerts"

    // Create channel on Android 8+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Sensor Alerts"
        val descriptionText = "Alerts for temperature, humidity, and particle levels"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Build notification
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure this exists
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    // Show it
    with(NotificationManagerCompat.from(context)) {
        notify(System.currentTimeMillis().toInt(), builder.build())//gives a red user might deny request
    }
}
