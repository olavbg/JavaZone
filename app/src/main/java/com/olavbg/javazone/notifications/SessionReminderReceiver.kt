package com.olavbg.javazone.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.olavbg.javazone.MainActivity
import com.olavbg.javazone.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SessionReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(ReminderManager.EXTRA_SESSION_ID) ?: return
        val title = intent.getStringExtra(ReminderManager.EXTRA_SESSION_TITLE) ?: "Session Reminder"
        val room = intent.getStringExtra(ReminderManager.EXTRA_SESSION_ROOM) ?: ""
        val startTimeZulu = intent.getStringExtra(ReminderManager.EXTRA_SESSION_START_TIME) ?: ""

        val startTimeFormatted = try {
            val instant = Instant.parse(startTimeZulu)
            val dateTime = instant.atZone(ZoneId.of("Europe/Oslo"))
            dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            ""
        }

        showNotification(context, sessionId, title, room, startTimeFormatted)
    }

    private fun showNotification(
        context: Context,
        sessionId: String,
        title: String,
        room: String,
        startTime: String
    ) {
        createNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("session_id", sessionId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Starts at $startTime in $room")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setSound(notificationSound)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(sessionId.hashCode(), notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Session Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for favorited sessions"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setSound(notificationSound, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "session_reminders_v2"
    }
}
