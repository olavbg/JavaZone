package com.olavbg.javazone.data.repository

import com.olavbg.javazone.data.local.FavoriteEntity
import com.olavbg.javazone.data.local.SessionDao
import com.olavbg.javazone.data.local.SessionEntity
import com.olavbg.javazone.data.remote.SessionDto
import com.olavbg.javazone.data.remote.SpeakerDto
import com.olavbg.javazone.data.remote.SleepingPillApi
import com.olavbg.javazone.model.Session
import com.olavbg.javazone.model.Speaker
import com.olavbg.javazone.notifications.ReminderManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class SessionRepository(
    private val api: SleepingPillApi,
    private val dao: SessionDao,
    private val reminderManager: ReminderManager? = null,
    private val settingsRepository: SettingsRepository? = null
) {
    fun getSessions(): Flow<List<Session>> {
        return dao.getAllSessions().combine(dao.getFavoriteSessionIds()) { sessions, favoriteIds ->
            sessions.map { entity ->
                entity.toDomainModel(isFavorite = favoriteIds.contains(entity.id))
            }
        }
    }

    suspend fun refreshSessions(conferenceId: String = "javazone_2026") {
        try {
            val response = api.getSessions(conferenceId)
            val entities = response.sessions.map { it.toEntity() }
            dao.insertSessions(entities)
            rescheduleAllFavorites()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun rescheduleAllFavorites() {
        val leadTime = settingsRepository?.notificationLeadTime?.first() ?: 10
        val timeOffset = settingsRepository?.simulatedTimeOffset?.first() ?: 0L
        val sessions = dao.getAllSessions().first()
        val favoriteIds = dao.getFavoriteSessionIds().first()

        sessions.filter { favoriteIds.contains(it.id) }.forEach { entity ->
            reminderManager?.scheduleReminder(entity.toDomainModel(true), leadTime, timeOffset)
        }
    }

    suspend fun toggleFavorite(sessionId: String, isFavorite: Boolean) {
        if (isFavorite) {
            dao.addFavorite(FavoriteEntity(sessionId))
            // Schedule reminder
            val sessions = dao.getAllSessions().first()
            val leadTime = settingsRepository?.notificationLeadTime?.first() ?: 10
            val timeOffset = settingsRepository?.simulatedTimeOffset?.first() ?: 0L
            sessions.find { it.id == sessionId }?.let { entity ->
                reminderManager?.scheduleReminder(entity.toDomainModel(true), leadTime, timeOffset)
            }
        } else {
            dao.removeFavorite(sessionId)
            // Cancel reminder
            val sessions = dao.getAllSessions().first()
            sessions.find { it.id == sessionId }?.let { entity ->
                reminderManager?.cancelReminder(entity.toDomainModel(false))
            }
        }
    }

    suspend fun getArchiveSessions(year: Int): List<Session> {
        return try {
            val response = api.getSessions("javazone_$year")
            response.sessions.map { dto ->
                Session(
                    id = dto.id,
                    title = dto.title ?: "",
                    abstract = dto.abstract ?: "",
                    room = dto.room ?: "",
                    startTimeZulu = dto.startTimeZulu ?: "",
                    endTimeZulu = dto.endTimeZulu ?: "",
                    format = dto.format ?: "",
                    language = dto.language,
                    videoUrl = dto.videoUrl,
                    speakers = dto.speakers?.map { Speaker(it.name, it.bio, it.twitter) } ?: emptyList(),
                    isFavorite = false
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun SessionDto.toEntity() = SessionEntity(
        id = id,
        title = title ?: "",
        abstractText = abstract ?: "",
        room = room ?: "",
        startTimeZulu = startTimeZulu ?: "",
        endTimeZulu = endTimeZulu ?: "",
        format = format ?: "",
        language = language,
        videoUrl = videoUrl,
        speakers = speakers?.map { it.toDomainModel() } ?: emptyList()
    )

    private fun SpeakerDto.toDomainModel() = Speaker(
        name = name,
        bio = bio,
        twitter = twitter
    )

    private fun SessionEntity.toDomainModel(isFavorite: Boolean) = Session(
        id = id,
        title = title,
        abstract = abstractText,
        room = room,
        startTimeZulu = startTimeZulu,
        endTimeZulu = endTimeZulu,
        format = format,
        language = language,
        videoUrl = videoUrl,
        speakers = speakers,
        isFavorite = isFavorite
    )
}
