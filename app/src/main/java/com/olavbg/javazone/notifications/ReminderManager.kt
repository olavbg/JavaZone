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
 * The alarm-scheduling surface of [ReminderManager], separated so the scheduling decisions
 * can be exercised without a real [AlarmManager].
 */
interface ReminderScheduler {
    fun scheduleReminder(session: Session, leadTimeMinutes: Int, timeOffsetMillis: Long)
    fun cancelReminder(session: Session)
    fun scheduleConferenceDoneReminder(conferenceEndMillis: Long, timeOffsetMillis: Long)
    fun cancelConferenceDoneReminder()
    fun showConferenceDoneNow()
}

/**
 * When a session reminder should fire in real wall-clock time, or null when it must not:
 * an unparseable start time, or a moment that has already passed. A simulated clock ahead of
 * real time shifts the alarm earlier so it still lands at the intended simulated moment.
 */
internal fun sessionReminderFireTimeMillis(
    startTimeZulu: String,
    leadTimeMinutes: Int,
    timeOffsetMillis: Long,
    nowMillis: Long
): Long? {
    val start = runCatching { Instant.parse(startTimeZulu).toEpochMilli() }.getOrNull() ?: return null
    val fireTime = start - leadTimeMinutes * 60_000L - timeOffsetMillis
    return if (fireTime <= nowMillis) null else fireTime
}

/** Same rule as [sessionReminderFireTimeMillis] for the "conference is over" notification. */
internal fun conferenceDoneFireTimeMillis(
    conferenceEndMillis: Long,
    timeOffsetMillis: Long,
    nowMillis: Long
): Long? {
    val fireTime = conferenceEndMillis - timeOffsetMillis
    return if (fireTime <= nowMillis) null else fireTime
}

/**
 * Decides what to do about the "current conference is over" notification for a given
 * conference end time and simulated-time offset:
 * - end time still in the (simulated) future -> arm the alarm. The year is deliberately left
 *   unclaimed, so that an alarm lost to a force stop or a missing exact-alarm permission is
 *   still recovered by the next check.
 * - end time already passed (real or simulated) -> post the notification right away, but only
 *   once per year. [ConferenceDoneReceiver] claims the year when the armed alarm fires.
 */
suspend fun handleConferenceDoneReminder(
    reminderScheduler: ReminderScheduler,
    settingsRepository: SettingsRepository?,
    conferenceEndMillis: Long?,
    timeOffsetMillis: Long,
    year: Int,
    nowMillis: Long = System.currentTimeMillis(),
) {
    if (conferenceEndMillis == null) {
        reminderScheduler.cancelConferenceDoneReminder()
        return
    }

    if (conferenceDoneFireTimeMillis(conferenceEndMillis, timeOffsetMillis, nowMillis) != null) {
        reminderScheduler.scheduleConferenceDoneReminder(conferenceEndMillis, timeOffsetMillis)
    } else {
        reminderScheduler.cancelConferenceDoneReminder()
        val settings = settingsRepository ?: return
        if (!settings.isConferenceDoneNotified(year)) {
            settings.markConferenceDoneNotified(year)
            reminderScheduler.showConferenceDoneNow()
        }
    }
}

class ReminderManager(context: Context) : ReminderScheduler {

    private val context = context.applicationContext
    private val alarmManager = this.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleReminder(session: Session, leadTimeMinutes: Int, timeOffsetMillis: Long) {
        val pendingIntent = buildPendingIntent(session)

        // Always cancel first so that re-scheduling replaces the old alarm even when the new
        // reminder time is in the real-time past (otherwise a stale alarm would keep firing).
        alarmManager.cancel(pendingIntent)

        val reminderTime = sessionReminderFireTimeMillis(
            startTimeZulu = session.startTimeZulu,
            leadTimeMinutes = leadTimeMinutes,
            timeOffsetMillis = timeOffsetMillis,
            nowMillis = System.currentTimeMillis()
        ) ?: return

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

    override fun scheduleConferenceDoneReminder(conferenceEndMillis: Long, timeOffsetMillis: Long) {
        val pendingIntent = buildConferenceDonePendingIntent()

        alarmManager.cancel(pendingIntent)

        val fireTime = conferenceDoneFireTimeMillis(
            conferenceEndMillis = conferenceEndMillis,
            timeOffsetMillis = timeOffsetMillis,
            nowMillis = System.currentTimeMillis()
        )
        if (fireTime == null) {
            Log.d(
                "JavaZoneNotifications",
                "ConferenceDone: skip (end=$conferenceEndMillis offset=$timeOffsetMillis already passed)"
            )
            return
        }

        Log.d(
            "JavaZoneNotifications",
            "ConferenceDone: end=$conferenceEndMillis offset=$timeOffsetMillis -> fire $fireTime (${Instant.ofEpochMilli(fireTime)})"
        )
        scheduleAlarm(fireTime, pendingIntent)
    }

    override fun cancelConferenceDoneReminder() {
        alarmManager.cancel(buildConferenceDonePendingIntent())
    }

    override fun showConferenceDoneNow() {
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

    override fun cancelReminder(session: Session) {
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
