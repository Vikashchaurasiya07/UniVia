package com.example.finalyearproject.navigation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.finalyearproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class NotificationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.failure()

        try {
            val snapshot = database.child("teacher_messages").get().await()

            var hasUnread = false
            for (messageSnapshot in snapshot.children) {
                val read =
                    messageSnapshot.child("readBy").child(uid).getValue(Boolean::class.java) == true
                if (!read) {
                    hasUnread = true
                    break
                }
            }

            if (hasUnread) {
                showNotification()
                Log.d("NotificationWorker", "Notification shown for unread messages.")
            } else {
                Log.d("NotificationWorker", "No unread messages.")
            }

            return Result.success()

        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error checking messages", e)
            return Result.failure()
        }
    }

    private fun showNotification() {
        val channelId = "teacher_messages"
        val notificationId = 1001

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Teacher Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from your teachers"
            }

            val manager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Teacher Notice")
            .setContentText("You have unread teacher messages. Tap to view.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            try {
                NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
            } catch (e: SecurityException) {
                Log.e("NotificationWorker", "Permission issue", e)
            }
        }
    }
}
