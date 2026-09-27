package com.olavbg.javazone.data.repository

import com.olavbg.javazone.support.FakeSessionDao
import com.olavbg.javazone.support.FakeSleepingPillApi
import com.olavbg.javazone.support.TimeoutRule
import com.olavbg.javazone.support.testSessionDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Covers the caching decisions in [SessionRepository]: not rewriting an unchanged program,
 * and not re-fetching an archive year that is already in memory.
 */
class RepositoryCacheTest {

    @get:Rule
    val timeoutRule = TimeoutRule()

    private val talk = testSessionDto(
        id = "cur-1",
        title = "Kotlin i produksjon",
        startTimeZulu = "2026-09-22T10:00:00Z",
        endTimeZulu = "2026-09-22T10:50:00Z"
    )

    @Test
    fun firstLoadWritesTheProgram() = runTest {
        val dao = FakeSessionDao()
        val repository = SessionRepository(FakeSleepingPillApi(currentYearSessions = listOf(talk)), dao)

        repository.refreshSessions()

        assertEquals(1, dao.replaceAllCalls)
    }

    @Test
    fun refetchingAnUnchangedProgramDoesNotRewriteTheTable() = runTest {
        val dao = FakeSessionDao()
        val repository = SessionRepository(FakeSleepingPillApi(currentYearSessions = listOf(talk)), dao)
        repository.refreshSessions()

        repository.refreshSessions()
        repository.refreshSessions()

        // Rewriting the table would restart the Room flows and make the agenda scroll jump.
        assertEquals(1, dao.replaceAllCalls)
    }

    @Test
    fun aChangedProgramIsWrittenEvenIfTheSessionCountMatches() = runTest {
        val dao = FakeSessionDao()
        val api = FakeSleepingPillApi(currentYearSessions = listOf(talk))
        val repository = SessionRepository(api, dao)
        repository.refreshSessions()

        api.currentYearSessions = listOf(talk.copy(title = "Ny tittel"))
        repository.refreshSessions()

        assertEquals(2, dao.replaceAllCalls)
        assertEquals("Ny tittel", repository.getSessions().first().single().title)
    }

    @Test
    fun aRemovedSessionIsReflectedAfterARefetch() = runTest {
        val dao = FakeSessionDao()
        val api = FakeSleepingPillApi(currentYearSessions = listOf(talk))
        val repository = SessionRepository(api, dao)
        repository.refreshSessions()

        api.currentYearSessions = emptyList()
        repository.refreshSessions()

        assertEquals(2, dao.replaceAllCalls)
        assertTrue(repository.getSessions().first().isEmpty())
    }

    @Test
    fun aFailingFetchLeavesTheCachedProgramIntact() = runTest {
        val dao = FakeSessionDao()
        val api = FakeSleepingPillApi(currentYearSessions = listOf(talk))
        val repository = SessionRepository(api, dao)
        repository.refreshSessions()

        api.sessionsError = IOException("offline")
        repository.refreshSessions()

        assertEquals(1, dao.replaceAllCalls)
        assertEquals("Kotlin i produksjon", repository.getSessions().first().single().title)
    }

    @Test
    fun anArchiveYearIsFetchedOnlyOnce() = runTest {
        val api = FakeSleepingPillApi(archiveSessionsByYear = mapOf(2025 to listOf(talk)))
        val repository = SessionRepository(api, FakeSessionDao())

        repository.loadArchiveSessions(2025)
        repository.loadArchiveSessions(2025)
        repository.loadArchiveSessions(2025)

        assertEquals(listOf("javazone_2025"), api.sessionCalls)
    }

    @Test
    fun loadingClearsTheLoadingFlagForTheYear() = runTest {
        val api = FakeSleepingPillApi(archiveSessionsByYear = mapOf(2025 to listOf(talk)))
        val repository = SessionRepository(api, FakeSessionDao())

        repository.loadArchiveSessions(2025)

        assertEquals(false, repository.archiveLoadingFlow().first()[2025])
        assertEquals(false, repository.archiveErrorFlow().first()[2025])
    }

    @Test
    fun aFailingArchiveFetchIsReportedAsAnErrorAndIsNotCached() = runTest {
        val api = FakeSleepingPillApi(
            archiveSessionsByYear = mapOf(2025 to listOf(talk)),
            sessionsError = IOException("offline")
        )
        val repository = SessionRepository(api, FakeSessionDao())

        repository.loadArchiveSessions(2025)

        assertEquals(true, repository.archiveErrorFlow().first()[2025])
        assertEquals(false, repository.archiveLoadingFlow().first()[2025])
    }

    @Test
    fun anEmptyArchiveYearIsCachedAsEmptyRatherThanRetriedForever() = runTest {
        val api = FakeSleepingPillApi()
        val repository = SessionRepository(api, FakeSessionDao())

        repository.loadArchiveSessions(2019)

        assertEquals(false, repository.archiveErrorFlow().first()[2019])
        // An empty-but-successful year is remembered, so a later visit does not re-fetch.
        repository.loadArchiveSessions(2019)
        assertEquals(listOf("javazone_2019"), api.sessionCalls)
    }

    @Test
    fun differentArchiveYearsAreTrackedIndependently() = runTest {
        val api = FakeSleepingPillApi(
            archiveSessionsByYear = mapOf(2025 to listOf(talk)),
            sessionsError = null
        )
        val repository = SessionRepository(api, FakeSessionDao())

        repository.loadArchiveSessions(2025)
        api.sessionsError = IOException("offline")
        repository.loadArchiveSessions(2024)

        assertEquals(false, repository.archiveErrorFlow().first()[2025])
        assertEquals(true, repository.archiveErrorFlow().first()[2024])
        assertFalse(repository.getSessionsFlow(2025).first().isEmpty())
        assertTrue(repository.getSessionsFlow(2024).first().isEmpty())
    }
}
