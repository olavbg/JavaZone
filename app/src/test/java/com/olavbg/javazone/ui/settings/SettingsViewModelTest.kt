package com.olavbg.javazone.ui.settings

import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.model.AppLanguage
import com.olavbg.javazone.model.BackgroundMode
import com.olavbg.javazone.model.ThemeMode
import com.olavbg.javazone.support.FakeSessionDao
import com.olavbg.javazone.support.FakeSleepingPillApi
import com.olavbg.javazone.support.MainDispatcherRule
import com.olavbg.javazone.support.TimeoutRule
import com.olavbg.javazone.support.createTestDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val timeoutRule = TimeoutRule()

    private fun buildViewModel(): Pair<SettingsViewModel, SettingsRepository> {
        val settingsRepository = SettingsRepository(createTestDataStore())
        val sessionRepository = SessionRepository(
            FakeSleepingPillApi(),
            FakeSessionDao(),
            null,
            settingsRepository
        )
        return SettingsViewModel(settingsRepository, sessionRepository) to settingsRepository
    }

    @Test
    fun defaults_areExposedImmediately() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()

        assertEquals(10, viewModel.notificationLeadTime.first())
        assertTrue(viewModel.notificationsEnabled.first())
        assertEquals(0L, viewModel.simulatedTimeOffset.first())
        assertEquals(BackgroundMode.Animated, viewModel.backgroundMode.first())
        assertEquals(ThemeMode.Dark, viewModel.themeMode.first())
        assertEquals(AppLanguage.System, viewModel.appLanguage.first())
    }

    @Test
    fun setNotificationLeadTime_persistsAndReschedules() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()

        viewModel.setNotificationLeadTime(30)

        assertEquals(30, viewModel.notificationLeadTime.first { it == 30 })
    }

    @Test
    fun setNotificationsEnabled_persistsAndReschedules() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()

        viewModel.setNotificationsEnabled(false)

        assertFalse(viewModel.notificationsEnabled.first { !it })
    }

    @Test
    fun simulatedTimeCanBeSet_andReset() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()

        viewModel.setSimulatedTime(java.time.LocalDateTime.of(2026, 9, 22, 14, 0))
        assertTrue(viewModel.simulatedTimeOffset.first { it != 0L } != 0L)

        viewModel.resetSimulation()
        assertEquals(0L, viewModel.simulatedTimeOffset.first { it == 0L })
    }

    @Test
    fun appearanceSettings_persistThroughViewModel() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()

        viewModel.setBackgroundMode(BackgroundMode.None)
        viewModel.setThemeMode(ThemeMode.System)

        assertEquals(BackgroundMode.None, viewModel.backgroundMode.first { it == BackgroundMode.None })
        assertEquals(ThemeMode.System, viewModel.themeMode.first { it == ThemeMode.System })
    }

    @Test
    fun setAppLanguage_persists() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()

        viewModel.setAppLanguage(AppLanguage.Norwegian)

        assertEquals(AppLanguage.Norwegian, viewModel.appLanguage.first { it == AppLanguage.Norwegian })
    }

    @Test
    fun dismissBatteryHint_persists() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, _) = buildViewModel()

        viewModel.dismissBatteryHint()

        assertTrue(viewModel.batteryHintDismissed.first { it })
    }
}