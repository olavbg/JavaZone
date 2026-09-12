package com.olavbg.javazone.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.olavbg.javazone.MainActivity
import com.olavbg.javazone.R
import com.olavbg.javazone.data.repository.SessionRepository

class ConferenceDoneReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("JavaZoneNotifications", "ConferenceDoneReceiver fired")
        showConferenceDoneNotification(context)
    }

    companion object {
        const val CHANNEL_ID = "conference_reminders_v2"
        const val CONFERENCE_DONE_NOTIFICATION_ID = 9001
        const val EXTRA_SHOW_DONATION_DIALOG = "show_donation_dialog"

        fun showConferenceDoneNotification(context: Context) {
            createNotificationChannel(context)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_SHOW_DONATION_DIALOG, true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                CONFERENCE_DONE_NOTIFICATION_ID,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("JavaZone ${SessionRepository.CURRENT_YEAR} er over")
                .setContentText("Hvordan syntes du app'en fungerte?")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            Log.d(
                "JavaZoneNotifications",
                "Posting conference-done notification to channel $CHANNEL_ID (id $CONFERENCE_DONE_NOTIFICATION_ID)"
            )
            notificationManager.notify(CONFERENCE_DONE_NOTIFICATION_ID, notification)
        }

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "JavaZone",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Informasjon om konferansen."
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}