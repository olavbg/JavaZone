package com.olavbg.javazone.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.olavbg.javazone.data.local.AppDatabase
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.model.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED") {
            val pendingResult = goAsync()
            rescheduleAlarms(context, pendingResult)
        }
    }

    private fun rescheduleAlarms(context: Context, pendingResult: PendingResult) {
        val database = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "javazone.db"
        ).fallbackToDestructiveMigration(true)
            .build()
        
        val reminderManager = ReminderManager(context)
        val settingsRepository = SettingsRepository(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            val sessions = database.sessionDao().getAllSessions().first()
            val favoriteIds = database.sessionDao().getFavoriteSessionIds().first()
            val leadTime = settingsRepository.notificationLeadTime.first()
            val timeOffset = settingsRepository.simulatedTimeOffset.first()
            
            sessions.filter { favoriteIds.contains(it.id) }.forEach { entity ->
                val session = Session(
                    id = entity.id,
                    title = entity.title,
                    abstract = entity.abstractText,
                    room = entity.room,
                    startTimeZulu = entity.startTimeZulu,
                    endTimeZulu = entity.endTimeZulu,
                    format = entity.format,
                    language = entity.language,
                    videoUrl = entity.videoUrl,
                    speakers = entity.speakers,
                    isFavorite = true
                )
                reminderManager.scheduleReminder(session, leadTime, timeOffset)
            }

            val maxEndMillis = sessions.mapNotNull {
                runCatching { Instant.parse(it.endTimeZulu).toEpochMilli() }.getOrNull()
            }.maxOrNull()
            handleConferenceDoneReminder(
                reminderManager = reminderManager,
                settingsRepository = settingsRepository,
                conferenceEndMillis = maxEndMillis,
                timeOffsetMillis = timeOffset,
                year = SessionRepository.CURRENT_YEAR
            )

            pendingResult.finish()
        }
    }
}
