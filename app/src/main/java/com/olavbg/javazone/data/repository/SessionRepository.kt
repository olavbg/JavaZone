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
import com.olavbg.javazone.notifications.handleConferenceDoneReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant

class SessionRepository(
    private val api: SleepingPillApi,
    private val dao: SessionDao,
    private val reminderManager: ReminderManager? = null,
    private val settingsRepository: SettingsRepository? = null
) {
    private val archiveSessions = MutableStateFlow<Map<Int, List<Session>>>(emptyMap())
    private val archiveLoading = MutableStateFlow<Map<Int, Boolean>>(emptyMap())

    private val _availableYears = MutableStateFlow(
        // Fallback until the conference list is fetched (offline/first launch)
        (2014..CURRENT_YEAR).reversed().toList()
    )
    val availableYears: StateFlow<List<Int>> = _availableYears.asStateFlow()

    fun getSessions(): Flow<List<Session>> {
        return getSessionsFlow(CURRENT_YEAR)
    }

    fun getSessionsFlow(year: Int = CURRENT_YEAR): Flow<List<Session>> {
        return if (year == CURRENT_YEAR) {
            dao.getAllSessions().combine(dao.getFavoriteSessionIds()) { sessions, favoriteIds ->
                sessions.map { entity ->
                    entity.toDomainModel(isFavorite = favoriteIds.contains(entity.id))
                }
            }
        } else {
            archiveSessions.map { map ->
                (map[year] ?: emptyList()).sortedBy { it.startTimeZulu }
            }
        }
    }

    fun archiveLoadingFlow(): Flow<Map<Int, Boolean>> = archiveLoading

    suspend fun loadAvailableYears() {
        try {
            val response = api.getConferences()
            val years = response.conferences.mapNotNull { conference ->
                conference.slug
                    ?.takeIf { it.startsWith("javazone_") }
                    ?.substringAfter("javazone_")
                    ?.toIntOrNull()
            }
                .let { list -> if (CURRENT_YEAR in list) list else list + CURRENT_YEAR }
                .distinct()
                .sortedDescending()
            if (years.isNotEmpty()) {
                _availableYears.value = years
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshSessions(conferenceId: String = "javazone_$CURRENT_YEAR") {
        try {
            val response = api.getSessions(conferenceId)
            val entities = response.sessions.map { it.toEntity() }
            dao.deleteAllSessions()
            dao.insertSessions(entities)
            rescheduleAllFavorites()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadArchiveSessions(year: Int) {
        if (archiveSessions.value.containsKey(year)) return
        archiveLoading.value = archiveLoading.value + (year to true)
        val sessions = fetchArchiveSessions(year)
        archiveSessions.value = archiveSessions.value + (year to sessions)
        archiveLoading.value = archiveLoading.value + (year to false)
    }

    suspend fun rescheduleAllFavorites() {
        val leadTime = settingsRepository?.notificationLeadTime?.first() ?: 10
        val timeOffset = settingsRepository?.simulatedTimeOffset?.first() ?: 0L
        val sessions = dao.getAllSessions().first()
        val favoriteIds = dao.getFavoriteSessionIds().first()

        sessions.filter { favoriteIds.contains(it.id) }.forEach { entity ->
            reminderManager?.scheduleReminder(entity.toDomainModel(true), leadTime, timeOffset)
        }
        rescheduleConferenceDoneReminder(timeOffset)
    }

    suspend fun rescheduleConferenceDoneReminder() {
        val timeOffset = settingsRepository?.simulatedTimeOffset?.first() ?: 0L
        rescheduleConferenceDoneReminder(timeOffset)
    }

    suspend fun rescheduleConferenceDoneReminder(timeOffset: Long) {
        val reminderManager = reminderManager ?: return
        val sessions = dao.getAllSessions().first()
        val maxEndMillis = sessions.mapNotNull {
            runCatching { Instant.parse(it.endTimeZulu).toEpochMilli() }.getOrNull()
        }.maxOrNull()

        handleConferenceDoneReminder(
            reminderManager = reminderManager,
            settingsRepository = settingsRepository,
            conferenceEndMillis = maxEndMillis,
            timeOffsetMillis = timeOffset,
            year = CURRENT_YEAR
        )
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

    private suspend fun fetchArchiveSessions(year: Int): List<Session> {
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

    companion object {
        const val CURRENT_YEAR = 2026

        // Fetched once from the JavaZone API on 2026-09-12. Archive years never change,
        // so these are kept up to date manually.
        val sessionCountsByYear: Map<Int, Int> = mapOf(
            2006 to 0,
            2007 to 0,
            2008 to 95,
            2009 to 121,
            2010 to 148,
            2011 to 129,
            2012 to 150,
            2013 to 149,
            2014 to 158,
            2015 to 164,
            2016 to 173,
            2017 to 140,
            2018 to 142,
            2019 to 139,
            2020 to 0,
            2021 to 111,
            2022 to 134,
            2023 to 137,
            2024 to 156,
            2025 to 137,
            2026 to 155,
        )
    }
}