package com.olavbg.javazone.ui.timeline

import androidx.lifecycle.viewModelScope
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.support.FakeSessionDao
import com.olavbg.javazone.support.FakeSleepingPillApi
import com.olavbg.javazone.support.MainDispatcherRule
import com.olavbg.javazone.support.TimeoutRule
import com.olavbg.javazone.support.createTestDataStore
import com.olavbg.javazone.support.englishDayName
import com.olavbg.javazone.support.fixedTimeTicks
import com.olavbg.javazone.support.testSessionDto
import com.olavbg.javazone.support.testSpeaker
import com.olavbg.javazone.support.zulu
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class TimelineViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val timeoutRule = TimeoutRule()

    private val today: LocalDate = LocalDate.now()
    private val tomorrow: LocalDate = today.plusDays(1)

    private val currentYearSessions = listOf(
        testSessionDto(
            id = "cur-1",
            title = "Kotlin i produksjon",
            abstract = "Alt om Kotlin i produksjon",
            room = "Room 1",
            startTimeZulu = zulu(today, "10:00"),
            endTimeZulu = zulu(today, "10:50"),
            format = "Presentation",
            language = "no",
            speakers = listOf(testSpeaker("Ola Nordmann"))
        ),
        testSessionDto(
            id = "cur-2",
            title = "Lightning Talks",
            abstract = "Korte og konsise talks",
            room = "Room 2",
            startTimeZulu = zulu(today, "11:00"),
            endTimeZulu = zulu(today, "11:20"),
            format = "Lightning Talk",
            language = "en"
        ),
        testSessionDto(
            id = "cur-3",
            title = "Compose Workshop",
            abstract = "Bygg skjermer med Compose",
            room = "Room 1",
            startTimeZulu = zulu(tomorrow, "09:00"),
            endTimeZulu = zulu(tomorrow, "11:00"),
            format = "Workshop",
            language = "en"
        )
    )

    private fun archiveSessions() = listOf(
        testSessionDto(
            id = "arch-1",
            title = "Arkivforedrag 2025",
            abstract = "Et foredrag fra 2025 med fullstendige detaljer",
            room = "Room 3",
            startTimeZulu = zulu(LocalDate.of(2025, 9, 3), "10:00"),
            endTimeZulu = zulu(LocalDate.of(2025, 9, 3), "10:50"),
            format = "Presentation",
            language = "en",
            videoUrl = "https://vimeo.com/123456",
            speakers = listOf(testSpeaker("Ada Lovelace"))
        )
    )

    private fun buildViewModel(
        api: FakeSleepingPillApi = FakeSleepingPillApi(currentYearSessions = currentYearSessions)
    ): Pair<TimelineViewModel, SettingsRepository> {
        val settingsRepository = SettingsRepository(createTestDataStore())
        val repository = SessionRepository(api, FakeSessionDao(), null, settingsRepository)
        val viewModel = TimelineViewModel(
            repository,
            settingsRepository,
            currentTimeTicks = fixedTimeTicks()
        )
        return viewModel to settingsRepository
    }

    /**
     * Tears the ViewModel down deterministically. We cancel and join the job so all
     * coroutines and completion handlers finish before the test scope and Main dispatcher
     * are dismantled.
     */
    private suspend fun dispose(viewModel: TimelineViewModel) {
        val job = viewModel.viewModelScope.coroutineContext[kotlinx.coroutines.Job]
        job?.cancel()
        job?.join()
    }

    @Test
    fun timeline_isLoadedAtStartup() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()

        val loaded = viewModel.sessions.first { it.isNotEmpty() }

        assertTrue(loaded.isNotEmpty())
        assertTrue(viewModel.allSessions.value.size >= 2)
        assertTrue(viewModel.groupedSessions.value.isNotEmpty())
        viewModel.isLoading.first { !it }
        assertEquals(
            setOf("cur-1", "cur-2", "cur-3"),
            viewModel.allSessions.first { it.isNotEmpty() }.map { it.id }.toSet()
        )
        dispose(viewModel)
    }

    @Test
    fun currentYearSessions_areExposedWithFullDetail() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()

        val kotlinTalk = viewModel.allSessions
            .first { it.isNotEmpty() }
            .first { it.id == "cur-1" }
        assertEquals("Kotlin i produksjon", kotlinTalk.title)
        assertEquals("Alt om Kotlin i produksjon", kotlinTalk.abstract)
        assertEquals("Room 1", kotlinTalk.room)
        assertTrue(kotlinTalk.speakers.any { it.name == "Ola Nordmann" })
        dispose(viewModel)
    }

    @Test
    fun switchingToEarlierYear_loadsThatYearAndKeepsDetails() =
        runTest(mainDispatcherRule.testDispatcher) {
            val api = FakeSleepingPillApi(
                currentYearSessions = currentYearSessions,
                archiveSessionsByYear = mapOf(2025 to archiveSessions())
            )
            val (viewModel, _) = buildViewModel(api)
            viewModel.isLoading.first { !it }

            viewModel.setYear(2025)

            val loaded = viewModel.sessions.first { it.isNotEmpty() }
            assertEquals(2025, viewModel.selectedYear.value)
            val session = loaded.single()
            assertEquals("Arkivforedrag 2025", session.title)
            assertEquals("Et foredrag fra 2025 med fullstendige detaljer", session.abstract)
            assertEquals("https://vimeo.com/123456", session.videoUrl)
            assertTrue(session.speakers.any { it.name == "Ada Lovelace" })
            dispose(viewModel)
        }

    @Test
    fun availableYears_areLoadedFromTheRepository() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()

        assertEquals(
            listOf(SessionRepository.CURRENT_YEAR),
            viewModel.availableYears.first { it.isNotEmpty() }
        )
        dispose(viewModel)
    }

    @Test
    fun dayFilter_limitsToSelectedDay() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()
        viewModel.selectedDay.first { it != null }

        viewModel.setDay(null)
        viewModel.setDay(englishDayName(tomorrow))

        val ids = viewModel.sessions.first { it.map { s -> s.id } == listOf("cur-3") }.map { it.id }
        assertEquals(listOf("cur-3"), ids)
        dispose(viewModel)
    }

    @Test
    fun formatFilter_limitsToSelectedFormat() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()
        viewModel.selectedDay.first { it != null }

        viewModel.setDay(null)
        viewModel.setFormat("Lightning Talk")

        val ids = viewModel.sessions.first { it.map { s -> s.id } == listOf("cur-2") }.map { it.id }
        assertEquals(listOf("cur-2"), ids)
        dispose(viewModel)
    }

    @Test
    fun roomFilter_limitsToSelectedRoom() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()
        viewModel.selectedDay.first { it != null }

        viewModel.setDay(null)
        viewModel.setRoom("Room 2")

        val ids = viewModel.sessions.first { it.map { s -> s.id } == listOf("cur-2") }.map { it.id }
        assertEquals(listOf("cur-2"), ids)
        dispose(viewModel)
    }

    @Test
    fun languageFilter_limitsToSelectedLanguage() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()
        viewModel.selectedDay.first { it != null }

        viewModel.setDay(null)
        viewModel.setLanguage("no")

        val ids = viewModel.sessions.first { it.map { s -> s.id } == listOf("cur-1") }.map { it.id }
        assertEquals(listOf("cur-1"), ids)
        dispose(viewModel)
    }

    @Test
    fun searchQuery_findsMatchingTitle() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()
        viewModel.selectedDay.first { it != null }

        viewModel.setDay(null)
        viewModel.setSearchQuery("compose")

        val ids = viewModel.sessions.first { it.map { s -> s.id } == listOf("cur-3") }.map { it.id }
        assertEquals(listOf("cur-3"), ids)
        dispose(viewModel)
    }

    @Test
    fun favoritesFilter_limitsToFavorites() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()
        viewModel.selectedDay.first { it != null }

        val target = viewModel.allSessions
            .first { it.any { s -> s.id == "cur-1" } }
            .first { it.id == "cur-1" }
        viewModel.toggleFavorite(target)
        viewModel.setDay(null)
        viewModel.setOnlyFavorites(true)

        val ids = viewModel.sessions.first { it.map { s -> s.id } == listOf("cur-1") }.map { it.id }
        assertEquals(listOf("cur-1"), ids)
        dispose(viewModel)
    }

    @Test
    fun combinedFilters_narrowDownTogether() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()
        viewModel.selectedDay.first { it != null }

        viewModel.setDay(null)
        viewModel.setFormat("Lightning Talk")
        viewModel.setRoom("Room 2")

        val ids = viewModel.sessions.first { it.map { s -> s.id } == listOf("cur-2") }.map { it.id }
        assertEquals(listOf("cur-2"), ids)
        dispose(viewModel)
    }
}