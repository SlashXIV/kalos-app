package com.kalos.app.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kalos.app.MainActivity
import com.kalos.app.R

object NotificationHelper {

    const val CHANNEL_ID = "kalos_workout_reminders"
    private const val CHANNEL_NAME = "Rappels d'entraînement"

    const val SMART_CHANNEL_ID = "kalos_smart_reminders"
    private const val SMART_CHANNEL_NAME = "Rappels intelligents"

    /** Intent extra read by MainActivity to deep-link a notification tap to a screen. */
    const val EXTRA_DESTINATION = "kalos_destination"
    const val DEST_NUTRITION = "nutrition"
    const val DEST_WORKOUT = "workout"
    const val DEST_WATER = "water"

    /** PendingIntent that opens the app on [destination] (or home when null). */
    private fun contentIntent(context: Context, notifId: Int, destination: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (destination != null) putExtra(EXTRA_DESTINATION, destination)
        }
        return PendingIntent.getActivity(
            context,
            notifId, // unique per notification type so extras don't collide
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Rappels pour les séances planifiées"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun createSmartChannel(context: Context) {
        val channel = NotificationChannel(
            SMART_CHANNEL_ID,
            SMART_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Rappels de discipline nutrition, sport et hydratation"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun postWorkoutReminder(
        context: Context,
        title: String,
        text: String,
        notifId: Int,
        destination: String? = DEST_WORKOUT,
    ) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, notifId, destination))
            .build()
        context.getSystemService(NotificationManager::class.java).notify(notifId, notification)
    }

    fun postSmartReminder(
        context: Context,
        title: String,
        text: String,
        notifId: Int,
        destination: String? = null,
    ) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, SMART_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, notifId, destination))
            .build()
        context.getSystemService(NotificationManager::class.java).notify(notifId, notification)
    }

    private fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}
