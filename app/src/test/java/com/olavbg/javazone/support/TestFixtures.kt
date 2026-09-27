package com.olavbg.javazone.support

import androidx.datastore.core.InterProcessCoordinator
import androidx.datastore.core.ReadScope
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import androidx.datastore.core.createSingleProcessCoordinator
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.olavbg.javazone.data.local.FavoriteEntity
import com.olavbg.javazone.data.local.SessionDao
import com.olavbg.javazone.data.local.SessionEntity
import com.olavbg.javazone.data.remote.ConferenceDto
import com.olavbg.javazone.data.remote.ConferencesResponseDto
import com.olavbg.javazone.data.remote.SessionDto
import com.olavbg.javazone.data.remote.SessionsResponseDto
import com.olavbg.javazone.data.remote.SleepingPillApi
import com.olavbg.javazone.data.remote.SpeakerDto
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.model.Session
import com.olavbg.javazone.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.rules.Timeout
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val OSLO_ZONE: ZoneId = ZoneId.of("Europe/Oslo")

fun zulu(date: LocalDate, time: String): String =
    date.atTime(LocalTime.parse(time)).atZone(OSLO_ZONE).toInstant().toString()

fun englishDayName(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))

fun testSessionDto(
    id: String,
    title: String = "Tittel $id",
    abstract: String? = "Sammendrag $id",
    room: String? = "Room 1",
    startTimeZulu: String,
    endTimeZulu: String = startTimeZulu,
    format: String? = "Presentation",
    language: String? = "no",
    videoUrl: String? = null,
    intendedAudience: String? = null,
    suggestedKeywords: String? = null,
    speakers: List<SpeakerDto>? = null
) = SessionDto(
    id = id,
    title = title,
    abstract = abstract,
    room = room,
    startTimeZulu = startTimeZulu,
    endTimeZulu = endTimeZulu,
    format = format,
    language = language,
    videoUrl = videoUrl,
    intendedAudience = intendedAudience,
    suggestedKeywords = suggestedKeywords,
    speakers = speakers
)

fun testSpeaker(name: String) =
    SpeakerDto(name = name, bio = null, twitter = null, bluesky = null, linkedin = null, pictureUrl = null)

fun testConference(slug: String) = ConferenceDto(id = "conference-$slug", name = null, slug = slug)

fun createTestDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        storage = InMemoryPreferenceStorage(),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    )

class FakeSleepingPillApi(
    var currentYearSessions: List<SessionDto> = emptyList(),
    var archiveSessionsByYear: Map<Int, List<SessionDto>> = emptyMap(),
    var conferences: List<ConferenceDto> = emptyList(),
    var sessionsError: Exception? = null
) : SleepingPillApi {

    val conferencesCalls = mutableListOf<String>()

    /** conferenceId per call, so a test can assert a year was fetched exactly once. */
    val sessionCalls = mutableListOf<String>()

    override suspend fun getConferences(): ConferencesResponseDto {
        conferencesCalls += "getConferences"
        return ConferencesResponseDto(conferences)
    }

    override suspend fun getSessions(conferenceId: String): SessionsResponseDto {
        sessionCalls += conferenceId
        sessionsError?.let { throw it }
        val year = conferenceId.removePrefix("javazone_").toIntOrNull()
        val sessions = if (year == SessionRepository.CURRENT_YEAR) {
            currentYearSessions
        } else {
            archiveSessionsByYear[year] ?: emptyList()
        }
        return SessionsResponseDto(sessions)
    }
}

class FakeSessionDao : SessionDao {
    private val sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    private val favorites = MutableStateFlow<List<String>>(emptyList())

    /** How often the repository wrote the whole table. 0 means the cached data was reused. */
    var replaceAllCalls = 0
        private set

    override fun getAllSessions(): Flow<List<SessionEntity>> = sessions

    override fun getFavoriteSessionIds(): Flow<List<String>> = favorites

    override suspend fun insertSessions(sessions: List<SessionEntity>) {
        this.sessions.value = this.sessions.value + sessions
    }

    override suspend fun deleteAllSessions() {
        sessions.value = emptyList()
    }

    override suspend fun replaceAllSessions(sessions: List<SessionEntity>) {
        replaceAllCalls++
        deleteAllSessions()
        insertSessions(sessions)
    }

    override suspend fun addFavorite(favorite: FavoriteEntity) {
        if (favorite.sessionId !in favorites.value) {
            favorites.value = favorites.value + favorite.sessionId
        }
    }

    override suspend fun removeFavorite(sessionId: String) {
        favorites.value = favorites.value - sessionId
    }
}

/** Records what would have been handed to [android.app.AlarmManager], in call order. */
class FakeReminderScheduler : ReminderScheduler {

    data class ScheduledSession(
        val sessionId: String,
        val leadTimeMinutes: Int,
        val timeOffsetMillis: Long
    )

    val scheduledSessions = mutableListOf<ScheduledSession>()
    val cancelledSessionIds = mutableListOf<String>()
    val scheduledConferenceDone = mutableListOf<Pair<Long, Long>>()
    var conferenceDoneCancels = 0
        private set
    var conferenceDoneShown = 0
        private set

    override fun scheduleReminder(session: Session, leadTimeMinutes: Int, timeOffsetMillis: Long) {
        scheduledSessions += ScheduledSession(session.id, leadTimeMinutes, timeOffsetMillis)
    }

    override fun cancelReminder(session: Session) {
        cancelledSessionIds += session.id
    }

    override fun scheduleConferenceDoneReminder(conferenceEndMillis: Long, timeOffsetMillis: Long) {
        scheduledConferenceDone += conferenceEndMillis to timeOffsetMillis
    }

    override fun cancelConferenceDoneReminder() {
        conferenceDoneCancels++
    }

    override fun showConferenceDoneNow() {
        conferenceDoneShown++
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

/**
 * Wall-clock cap for a single test. `runTest`'s own timeout does not cover the untimed
 * `advanceUntilIdle()` it runs in its `finally` block, so an endless repeating task in the
 * test scope would otherwise hang the build with no way out. JUnit runs the test on a daemon
 * thread, so a runaway loop cannot keep the worker JVM alive either.
 */
class TimeoutRule(private val timeoutSeconds: Long = 30) : TestRule {
    override fun apply(base: Statement, description: Description): Statement =
        Timeout.seconds(timeoutSeconds).apply(base, description)
}

/** A finite stand-in for the endless [com.olavbg.javazone.util.minuteTicks]. */
fun fixedTimeTicks(instant: Instant = Instant.now()): Flow<Instant> = flowOf(instant)

class InMemoryPreferenceStorage(
    initialValue: Preferences = emptyPreferences()
) : Storage<Preferences> {

    @Volatile
    private var value: Preferences = initialValue

    private val mutex = Mutex()

    override fun createConnection(): StorageConnection<Preferences> =
        object : StorageConnection<Preferences> {
            override val coordinator: InterProcessCoordinator =
                createSingleProcessCoordinator("in-memory")

            override suspend fun <R> readScope(
                block: suspend ReadScope<Preferences>.(locked: Boolean) -> R
            ): R {
                val snapshot = value
                return object : ReadScope<Preferences> {
                    override suspend fun readData(): Preferences = snapshot
                    override fun close() {}
                }.let { scope -> scope.block(true) }
            }

            override suspend fun writeScope(block: suspend WriteScope<Preferences>.() -> Unit) {
                mutex.withLock {
                    val snapshot = value
                    val scope = object : WriteScope<Preferences> {
                        override suspend fun readData(): Preferences = snapshot
                        override suspend fun writeData(newValue: Preferences) { value = newValue }
                        override fun close() {}
                    }
                    scope.block()
                }
            }

            override fun close() {}
        }
}