package com.olavbg.javazone.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.model.Session
import java.time.Instant

/**
 * Decides what to do about the "current conference is over" notification for a given
 * conference end time and simulated-time offset:
 * - end time still in the (simulated) future  -> schedule the alarm.
 * - end time already passed (real or simulated) -> post the notification right away,
 *   but only once per year.
 */
suspend fun handleConferenceDoneReminder(
    reminderManager: ReminderManager,
    settingsRepository: SettingsRepository?,
    conferenceEndMillis: Long?,
    timeOffsetMillis: Long,
    year: Int,
) {
    if (conferenceEndMillis == null) {
        reminderManager.cancelConferenceDoneReminder()
        return
    }

    val fireTime = conferenceEndMillis - timeOffsetMillis

    if (fireTime > System.currentTimeMillis()) {
        reminderManager.scheduleConferenceDoneReminder(conferenceEndMillis, timeOffsetMillis)
        settingsRepository?.markConferenceDoneNotified(year)
    } else {
        reminderManager.cancelConferenceDoneReminder()
        val notified = settingsRepository?.isConferenceDoneNotified(year) ?: true
        if (!notified) {
            settingsRepository?.markConferenceDoneNotified(year)
            reminderManager.showConferenceDoneNow()
        }
    }
}

class ReminderManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(session: Session, leadTimeMinutes: Int = 10, timeOffsetMillis: Long = 0L) {
        val startTime = try {
            Instant.parse(session.startTimeZulu).toEpochMilli()
        } catch (e: Exception) {
            return
        }

        val pendingIntent = buildPendingIntent(session)

        // Always cancel first so that re-scheduling replaces the old alarm even when the new
        // reminder time is in the real-time past (otherwise a stale alarm would keep firing).
        alarmManager.cancel(pendingIntent)

        // Adjust the reminder time by the simulated offset.
        // If simulated time is 1 hour ahead, the alarm should fire 1 hour earlier in real time.
        val reminderTime = startTime - (leadTimeMinutes * 60 * 1000) - timeOffsetMillis

        if (reminderTime <= System.currentTimeMillis()) {
            return // Already past in real time (or in simulated time); no new alarm to schedule
        }

        scheduleAlarm(reminderTime, pendingIntent)
    }

    private fun buildPendingIntent(session: Session): PendingIntent {
        val intent = Intent(context, SessionReminderReceiver::class.java).apply {
            putExtra(EXTRA_SESSION_ID, session.id)
            putExtra(EXTRA_SESSION_TITLE, session.title)
            putExtra(EXTRA_SESSION_ROOM, session.room)
            putExtra(EXTRA_SESSION_START_TIME, session.startTimeZulu)
        }

        return PendingIntent.getBroadcast(
            context,
            session.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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

    fun scheduleConferenceDoneReminder(conferenceEndMillis: Long, timeOffsetMillis: Long = 0L) {
        val pendingIntent = buildConferenceDonePendingIntent()

        alarmManager.cancel(pendingIntent)

        // Same simulated-time adjustment as session reminders: a future-simulated clock
        // shifts the alarm earlier in real time so it fires at the right simulated moment.
        val fireTime = conferenceEndMillis - timeOffsetMillis

        if (fireTime <= System.currentTimeMillis()) {
            Log.d(
                "JavaZoneNotifications",
                "ConferenceDone: skip (fireTime ${Instant.ofEpochMilli(fireTime)} already passed)"
            )
            return // Conference already over (in real or simulated time); no notification
        }

        Log.d(
            "JavaZoneNotifications",
            "ConferenceDone: end=${Instant.ofEpochMilli(conferenceEndMillis)} offset=$timeOffsetMillis -> fire $fireTime (${Instant.ofEpochMilli(fireTime)})"
        )
        scheduleAlarm(fireTime, pendingIntent)
    }

    fun cancelConferenceDoneReminder() {
        alarmManager.cancel(buildConferenceDonePendingIntent())
    }

    fun showConferenceDoneNow() {
        ConferenceDoneReceiver.showConferenceDoneNotification(context)
    }

    private fun buildConferenceDonePendingIntent(): PendingIntent {
        val intent = Intent(context, ConferenceDoneReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            CONFERENCE_DONE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelReminder(session: Session) {
        alarmManager.cancel(buildPendingIntent(session))
    }

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_SESSION_TITLE = "session_title"
        const val EXTRA_SESSION_ROOM = "session_room"
        const val EXTRA_SESSION_START_TIME = "session_start_time"
        const val CONFERENCE_DONE_REQUEST_CODE = 9001
    }
}
