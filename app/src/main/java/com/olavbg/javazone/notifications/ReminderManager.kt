package com.olavbg.javazone.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.olavbg.javazone.model.Session
import java.time.Instant

class ReminderManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(session: Session, leadTimeMinutes: Int = 10, timeOffsetMillis: Long = 0L) {
        val startTime = try {
            Instant.parse(session.startTimeZulu).toEpochMilli()
        } catch (e: Exception) {
            return
        }

        // Adjust the reminder time by the simulated offset.
        // If simulated time is 1 hour ahead, the alarm should fire 1 hour earlier in real time.
        val reminderTime = startTime - (leadTimeMinutes * 60 * 1000) - timeOffsetMillis

        if (reminderTime <= System.currentTimeMillis()) {
            return // Already past in real time
        }

        val intent = Intent(context, SessionReminderReceiver::class.java).apply {
            putExtra(EXTRA_SESSION_ID, session.id)
            putExtra(EXTRA_SESSION_TITLE, session.title)
            putExtra(EXTRA_SESSION_ROOM, session.room)
            putExtra(EXTRA_SESSION_START_TIME, session.startTimeZulu)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            session.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(reminderTime, pendingIntent)
    }

    private fun scheduleAlarm(timeMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                // Use setAlarmClock for maximum reliability. 
                // It's less likely to be deferred by the system than setExactAndAllowWhileIdle.
                val info = AlarmManager.AlarmClockInfo(timeMillis, pendingIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
            } else {
                // Fallback to inexact if permission not granted
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pendingIntent
                )
            }
        } else {
            val info = AlarmManager.AlarmClockInfo(timeMillis, pendingIntent)
            alarmManager.setAlarmClock(info, pendingIntent)
        }
    }

    fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun cancelReminder(session: Session) {
        val intent = Intent(context, SessionReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            session.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_SESSION_TITLE = "session_title"
        const val EXTRA_SESSION_ROOM = "session_room"
        const val EXTRA_SESSION_START_TIME = "session_start_time"
    }
}
