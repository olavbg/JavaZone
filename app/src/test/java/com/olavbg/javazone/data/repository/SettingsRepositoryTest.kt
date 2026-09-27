package com.olavbg.javazone.data.repository

import com.olavbg.javazone.model.AppLanguage
import com.olavbg.javazone.model.BackgroundMode
import com.olavbg.javazone.model.ThemeMode
import com.olavbg.javazone.support.TimeoutRule
import com.olavbg.javazone.support.createTestDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsRepositoryTest {

    @get:Rule
    val timeoutRule = TimeoutRule()

    private fun newRepository(): SettingsRepository =
        SettingsRepository(createTestDataStore())

    @Test
    fun defaults_areSaneOnEmptyStorage() = runTest {
        val repository = newRepository()

        assertEquals(10, repository.notificationLeadTime.first())
        assertTrue(repository.notificationsEnabled.first())
        assertEquals(0L, repository.simulatedTimeOffset.first())
        assertEquals(BackgroundMode.Animated, repository.backgroundMode.first())
        assertEquals(ThemeMode.Dark, repository.themeMode.first())
        assertEquals(AppLanguage.System, repository.appLanguage.first())
        assertFalse(repository.filtersExpanded.first())
        assertNull(repository.timelineFilters.first())
    }

    @Test
    fun notificationSettings_roundTrip() = runTest {
        val repository = newRepository()

        repository.updateNotificationLeadTime(45)
        repository.updateNotificationsEnabled(false)

        assertEquals(45, repository.notificationLeadTime.first())
        assertFalse(repository.notificationsEnabled.first())
    }

    @Test
    fun appearanceSettings_roundTrip() = runTest {
        val repository = newRepository()

        repository.updateBackgroundMode(BackgroundMode.Static)
        repository.updateThemeMode(ThemeMode.Light)
        repository.updateAppLanguage(AppLanguage.English)

        assertEquals(BackgroundMode.Static, repository.backgroundMode.first())
        assertEquals(ThemeMode.Light, repository.themeMode.first())
        assertEquals(AppLanguage.English, repository.appLanguage.first())
    }

    @Test
    fun simulatedTimeOffset_roundTrip() = runTest {
        val repository = newRepository()

        repository.updateSimulatedTimeOffset(7_200_000L)

        assertEquals(7_200_000L, repository.simulatedTimeOffset.first())
    }

    @Test
    fun timelineFilters_saveAndClear() = runTest {
        val repository = newRepository()
        val filters = TimelineFilters(
            savedForYear = SessionRepository.CURRENT_YEAR,
            selectedDay = "Wednesday",
            selectedRoom = "Room 1",
            selectedFormat = "Lightning Talk",
            selectedLanguage = "en",
            onlyFavorites = true
        )

        repository.saveTimelineFilters(filters)

        val saved = repository.timelineFilters.first()
        assertEquals(filters, saved)

        repository.clearTimelineFilters()

        assertNull(repository.timelineFilters.first())
    }

    @Test
    fun filtersExpanded_flagRoundTrips() = runTest {
        val repository = newRepository()

        repository.setFiltersExpanded(true)

        assertTrue(repository.filtersExpanded.first())
    }

    @Test
    fun firedReminderIds_accumulate() = runTest {
        val repository = newRepository()

        repository.markSessionReminderFired("a")
        repository.markSessionReminderFired("b")

        assertEquals(setOf("a", "b"), repository.firedReminderSessionIds.first())
    }

    @Test
    fun notificationPromptAndBatteryHint_roundTrip() = runTest {
        val repository = newRepository()

        repository.markNotificationPromptShown()
        repository.dismissBatteryHint()
        repository.dismissFavoriteHint()
        repository.dismissYearArchiveHint()

        assertTrue(repository.notificationPromptShown.first())
        assertTrue(repository.batteryHintDismissed.first())
        assertTrue(repository.favoriteHintDismissed.first())
        assertTrue(repository.yearArchiveHintDismissed.first())
    }

    @Test
    fun conferenceDoneNotification_flagIsSeparatedPerYear() = runTest {
        val repository = newRepository()

        assertFalse(repository.isConferenceDoneNotified(2025))
        repository.markConferenceDoneNotified(2025)

        assertTrue(repository.isConferenceDoneNotified(2025))
        assertFalse(repository.isConferenceDoneNotified(2026))
    }
}