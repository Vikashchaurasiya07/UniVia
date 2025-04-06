package com.example.finalyearproject.navigation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.finalyearproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun doWork(): Result {
        val uid = auth.currentUser?.uid

        if (uid != null) {
            database.child("teacher_messages").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasUnread = false
                    for (messageSnapshot in snapshot.children) {
                        val read = messageSnapshot.child("readBy").child(uid).getValue(Boolean::class.java) == true
                        if (!read) {
                            hasUnread = true
                            break
                        }
                    }

                    if (hasUnread) {
                        showNotification()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error if needed
                }
            })
        }

        return Result.success()
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

            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Teacher Notice")
            .setContentText("You have unread teacher messages. Tap to view.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // ✅ Check for notification permission (Android 13+)
        if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            try {
                NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}
