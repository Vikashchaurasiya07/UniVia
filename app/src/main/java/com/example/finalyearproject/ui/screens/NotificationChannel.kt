package com.example.finalyearproject.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

fun createNotificationChannel(context: Context) {
    // Check for Android version and create a notification channel if necessary
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "teacher_message_channel"
        val channelName = "Teacher Notifications"
        val channelDescription = "Notifications for teacher's messages"
        val importance = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = channelDescription
        }

        // Register the channel with the system
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
