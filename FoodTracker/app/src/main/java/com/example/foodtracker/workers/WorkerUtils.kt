package com.example.foodtracker.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.foodtracker.R
import com.example.foodtracker.ui.MainActivity


const val CHANNEL_ID = "VERBOSE_NOTIFICATION"
const val VERBOSE_NOTIFICATION_CHANNEL_NAME = "Expiration Reminders"
const val VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION = "Shows reminders about soon-to-expire products"
const val NOTIFICATION_ID = 1
const val REQUEST_CODE = 0


fun makeProductExpiryReminder(title: String, message: String, context: Context) {
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel(CHANNEL_ID, VERBOSE_NOTIFICATION_CHANNEL_NAME, importance)
    channel.description = VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

    notificationManager?.createNotificationChannel(channel)

    val pendingIntent = createPendingIntent(context)

    val notification = Notification.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(message)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    notificationManager?.notify(NOTIFICATION_ID, notification)
}

fun createPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    return pendingIntent
}
