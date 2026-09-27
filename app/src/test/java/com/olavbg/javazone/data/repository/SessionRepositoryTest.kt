package com.olavbg.javazone.data.repository

import com.olavbg.javazone.support.FakeSessionDao
import com.olavbg.javazone.support.FakeSleepingPillApi
import com.olavbg.javazone.support.TimeoutRule
import com.olavbg.javazone.support.testConference
import com.olavbg.javazone.support.testSessionDto
import com.olavbg.javazone.support.testSpeaker
import com.olavbg.javazone.support.zulu
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class SessionRepositoryTest {

    @get:Rule
    val timeoutRule = TimeoutRule()

    private val currentYearSessions = listOf(
        testSessionDto(
            id = "cur-1",
            title = "Kotlin i produksjon",
            abstract = "Alt om Kotlin",
            room = "Room 1",
            startTimeZulu = zulu(LocalDate.of(2026, 9, 22), "10:00"),
            endTimeZulu = zulu(LocalDate.of(2026, 9, 22), "10:50"),
            format = "Presentation",
            language = "no",
            speakers = listOf(testSpeaker("Ola Nordmann"))
        )
    )

    @Test
    fun getSessions_returnsCurrentYearSessionsFetchedFromApi() = runTest {
        val api = FakeSleepingPillApi(currentYearSessions = currentYearSessions)
        val repository = SessionRepository(api, FakeSessionDao())

        repository.refreshSessions()

        val sessions = repository.getSessions().first()
        assertEquals(1, sessions.size)
        assertEquals("Kotlin i produksjon", sessions.first().title)
        assertTrue(sessions.first().speakers.any { it.name == "Ola Nordmann" })
    }

    @Test
    fun toggleFavorite_marksSessionAsFavorite_andUpdatesCount() = runTest {
        val api = FakeSleepingPillApi(currentYearSessions = currentYearSessions)
        val repository = SessionRepository(api, FakeSessionDao())
        repository.refreshSessions()

        repository.toggleFavorite("cur-1", true)

        val favorite = repository.getSessions().first { it.any { s -> s.isFavorite } }
        assertTrue(favorite.first().isFavorite)
        assertEquals(1, repository.favoriteCount.first())

        repository.toggleFavorite("cur-1", false)

        val unfavorited = repository.getSessions().first { it.none { s -> s.isFavorite } }
        assertTrue(unfavorited.none { it.isFavorite })
        assertEquals(0, repository.favoriteCount.first())
    }

    @Test
    fun loadAvailableYears_parsesConferencesSortedDescending() = runTest {
        val api = FakeSleepingPillApi(
            conferences = listOf(
                testConference("javazone_2024"),
                testConference("javazone_2026"),
                testConference("javazone_2025"),
                testConference("some_other_conference")
            )
        )
        val repository = SessionRepository(api, FakeSessionDao())

        repository.loadAvailableYears()

        assertEquals(listOf(2026, 2025, 2024), repository.availableYears.value)
    }

    @Test
    fun loadAvailableYears_injectsCurrentYear_whenApiReturnsNoJavazoneConferences() = runTest {
        val repository = SessionRepository(FakeSleepingPillApi(), FakeSessionDao())

        repository.loadAvailableYears()

        assertEquals(listOf(SessionRepository.CURRENT_YEAR), repository.availableYears.value)
    }

    @Test
    fun availableYears_fallBackToKnownRange_beforeFirstLoad() {
        val repository = SessionRepository(FakeSleepingPillApi(), FakeSessionDao())

        assertEquals(
            (2014..SessionRepository.CURRENT_YEAR).reversed().toList(),
            repository.availableYears.value
        )
    }

    @Test
    fun archiveYear_exposesFullDetailForDetailScreenLookup() = runTest {
        val archiveYear = 2025
        val archiveSession = testSessionDto(
            id = "arch-1",
            title = "Arkivforedrag 2025",
            abstract = "Et foredrag fra 2025 med fullstendige detaljer",
            room = "Room 3",
            startTimeZulu = zulu(LocalDate.of(2025, 9, 3), "10:00"),
            endTimeZulu = zulu(LocalDate.of(2025, 9, 3), "10:50"),
            format = "Presentation",
            language = "en",
            videoUrl = "https://vimeo.com/123456",
            intendedAudience = "Utviklere",
            suggestedKeywords = "kotlin; compose",
            speakers = listOf(testSpeaker("Ada Lovelace"))
        )
        val api = FakeSleepingPillApi(
            currentYearSessions = currentYearSessions,
            archiveSessionsByYear = mapOf(archiveYear to listOf(archiveSession))
        )
        val repository = SessionRepository(api, FakeSessionDao())

        repository.loadArchiveSessions(archiveYear)

        // This mirrors how SessionDetailScreen looks up a talk before rendering it.
        val session = repository.getSessionsFlow(archiveYear)
            .first { it.isNotEmpty() }
            .find { it.id == "arch-1" }
        assertNotNull(session)
        assertEquals("Et foredrag fra 2025 med fullstendige detaljer", session!!.abstract)
        assertEquals("https://vimeo.com/123456", session.videoUrl)
        assertEquals("Utviklere", session.intendedAudience)
        assertTrue(session.suggestedKeywords!!.contains("compose"))
        assertTrue(session.speakers.any { it.name == "Ada Lovelace" })
    }

    @Test
    fun archiveYear_unknownSessionReturnsNull_forDetailLookup() = runTest {
        val archiveYear = 2025
        val api = FakeSleepingPillApi(
            currentYearSessions = currentYearSessions,
            archiveSessionsByYear = mapOf(archiveYear to listOf(currentYearSessions.first()))
        )
        val repository = SessionRepository(api, FakeSessionDao())
        repository.loadArchiveSessions(archiveYear)

        repository.getSessionsFlow(archiveYear).first { it.isNotEmpty() }

        val missing = repository.getSessionsFlow(archiveYear)
            .first()
            .find { it.id == "does-not-exist" }
        assertNull(missing)
    }
}