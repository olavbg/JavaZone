package com.olavbg.javazone.notifications

import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.support.FakeReminderScheduler
import com.olavbg.javazone.support.FakeSessionDao
import com.olavbg.javazone.support.FakeSleepingPillApi
import com.olavbg.javazone.support.TimeoutRule
import com.olavbg.javazone.support.createTestDataStore
import com.olavbg.javazone.support.testSessionDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * The reminder math and the scheduling decisions it feeds. Getting the simulated-clock
 * offset wrong loses notifications silently, so the boundaries are pinned down here.
 */
class ReminderSchedulingTest {

    @get:Rule
    val timeoutRule = TimeoutRule()

    private val now = 1_800_000_000_000L
    private val halfHour = 30 * 60_000L
    private val oneHour = 60 * 60_000L
    private val twoHours = 2 * oneHour

    private fun startIso(offsetMillis: Long = oneHour) =
        Instant.ofEpochMilli(now + offsetMillis).toString()

    private fun settingsRepository() = SettingsRepository(createTestDataStore())

    // --- pure fire-time math -----------------------------------------------------------------

    @Test
    fun fireTimeSubtractsTheConfiguredLeadTime() {
        assertEquals(
            now + twoHours - 10 * 60_000L,
            sessionReminderFireTimeMillis(startIso(twoHours), 10, 0L, now)
        )
    }

    @Test
    fun aSimulatedClockAheadOfRealTimeShiftsTheAlarmEarlier() {
        // A simulated clock running ahead means the same simulated moment arrives earlier in
        // real time, so the alarm has to be set earlier to land on it.
        assertEquals(
            now + twoHours - 10 * 60_000L - halfHour,
            sessionReminderFireTimeMillis(startIso(twoHours), 10, halfHour, now)
        )
    }

    @Test
    fun aSimulatedClockBehindRealTimeShiftsTheAlarmLater() {
        assertEquals(
            now + twoHours - 10 * 60_000L + halfHour,
            sessionReminderFireTimeMillis(startIso(twoHours), 10, -halfHour, now)
        )
    }

    @Test
    fun anOffsetThatWouldFireInTheRealPastIsSkipped() {
        // A simulated clock an hour ahead maps this reminder to ten minutes ago in real time,
        // which is not schedulable.
        assertNull(sessionReminderFireTimeMillis(startIso(), 10, oneHour, now))
    }

    @Test
    fun fireTimeIsNullOnceTheMomentHasPassed() {
        assertNull(sessionReminderFireTimeMillis(startIso(offsetMillis = -oneHour), 10, 0L, now))
    }

    @Test
    fun fireTimeIsNullExactlyAtTheMomentItWouldFire() {
        assertNull(sessionReminderFireTimeMillis(startIso(offsetMillis = 10 * 60_000L), 10, 0L, now))
    }

    @Test
    fun fireTimeIsNullWhenTheStartTimeCannotBeParsed() {
        assertNull(sessionReminderFireTimeMillis("not-a-time", 10, 0L, now))
    }

    @Test
    fun conferenceDoneFireTimeShiftsByTheSimulatedOffset() {
        assertEquals(now + 10_000L, conferenceDoneFireTimeMillis(now + oneHour, oneHour - 10_000L, now))
    }

    @Test
    fun conferenceDoneFireTimeIsNullOnceTheEndHasPassed() {
        assertNull(conferenceDoneFireTimeMillis(now - 1L, 0L, now))
    }

    // --- the conference-done decision table --------------------------------------------------

    @Test
    fun anUnknownConferenceEndCancelsAPendingAlarm() = runTest {
        val scheduler = FakeReminderScheduler()

        handleConferenceDoneReminder(scheduler, settingsRepository(), null, 0L, 2026, now)

        assertEquals(1, scheduler.conferenceDoneCancels)
        assertTrue(scheduler.scheduledConferenceDone.isEmpty())
    }

    @Test
    fun aFutureConferenceArmsTheAlarmWithoutClaimingTheYear() = runTest {
        val scheduler = FakeReminderScheduler()
        val settings = settingsRepository()

        handleConferenceDoneReminder(scheduler, settings, now + oneHour, 0L, 2026, now)

        assertEquals(listOf((now + oneHour) to 0L), scheduler.scheduledConferenceDone)
        assertEquals(0, scheduler.conferenceDoneCancels)
        assertEquals(0, scheduler.conferenceDoneShown)
        // Claiming the year here would silently drop the notification if the alarm never fired.
        assertFalse(settings.isConferenceDoneNotified(2026))
    }

    @Test
    fun aConferenceThatAlreadyEndedPostsTheNotificationOnce() = runTest {
        val scheduler = FakeReminderScheduler()
        val settings = settingsRepository()

        handleConferenceDoneReminder(scheduler, settings, now - 1L, 0L, 2026, now)

        assertEquals(1, scheduler.conferenceDoneShown)
        assertEquals(1, scheduler.conferenceDoneCancels)
        assertTrue(settings.isConferenceDoneNotified(2026))
    }

    @Test
    fun anAlarmThatNeverFiredIsRecoveredOnTheNextCheck() = runTest {
        val scheduler = FakeReminderScheduler()
        val settings = settingsRepository()

        // A normal app start before the conference ends: the alarm is armed, the year unclaimed.
        handleConferenceDoneReminder(scheduler, settings, now + oneHour, 0L, 2026, now)
        assertFalse(settings.isConferenceDoneNotified(2026))

        // The alarm was lost to a force stop or a missing exact-alarm permission, and the
        // conference is now over.
        handleConferenceDoneReminder(scheduler, settings, now - 1L, 0L, 2026, now)

        assertEquals(1, scheduler.conferenceDoneShown)
        assertTrue(settings.isConferenceDoneNotified(2026))
    }

    @Test
    fun aConferenceThatAlreadyEndedStaysSilentForAYearAlreadyClaimed() = runTest {
        val scheduler = FakeReminderScheduler()
        val settings = settingsRepository()
        // As left behind by ConferenceDoneReceiver once the armed alarm has fired.
        settings.markConferenceDoneNotified(2026)

        handleConferenceDoneReminder(scheduler, settings, now - 1L, 0L, 2026, now)

        assertEquals(0, scheduler.conferenceDoneShown)
        assertEquals(1, scheduler.conferenceDoneCancels)
    }

    @Test
    fun withoutSettingsAnEndedConferenceIsTreatedAsAlreadyNotified() = runTest {
        val scheduler = FakeReminderScheduler()

        handleConferenceDoneReminder(scheduler, null, now - 1L, 0L, 2026, now)

        assertEquals(0, scheduler.conferenceDoneShown)
    }

    // --- the repository wiring ----------------------------------------------------------------

    private fun repositoryWithTalk(
        scheduler: FakeReminderScheduler,
        settings: SettingsRepository
    ): SessionRepository {
        val api = FakeSleepingPillApi(
            currentYearSessions = listOf(
                testSessionDto(
                    id = "cur-1",
                    title = "Kotlin i produksjon",
                    startTimeZulu = Instant.ofEpochMilli(now + oneHour).toString(),
                    endTimeZulu = Instant.ofEpochMilli(now + oneHour + 50 * 60_000L).toString()
                )
            )
        )
        return SessionRepository(api, FakeSessionDao(), scheduler, settings)
    }

    @Test
    fun favouritingATalkSchedulesItsReminder() = runTest {
        val scheduler = FakeReminderScheduler()
        val settings = settingsRepository()
        settings.updateNotificationLeadTime(30)
        val repository = repositoryWithTalk(scheduler, settings)
        repository.refreshSessions()

        repository.toggleFavorite("cur-1", true)

        val scheduled = scheduler.scheduledSessions.single()
        assertEquals("cur-1", scheduled.sessionId)
        assertEquals(30, scheduled.leadTimeMinutes)
    }

    @Test
    fun unfavouritingATalkCancelsItsReminder() = runTest {
        val scheduler = FakeReminderScheduler()
        val settings = settingsRepository()
        val repository = repositoryWithTalk(scheduler, settings)
        repository.refreshSessions()
        repository.toggleFavorite("cur-1", true)

        repository.toggleFavorite("cur-1", false)

        assertEquals(listOf("cur-1"), scheduler.cancelledSessionIds)
    }

    @Test
    fun reschedulingPassesTheStoredLeadTimeAndOffsetAlong() = runTest {
        val scheduler = FakeReminderScheduler()
        val settings = settingsRepository()
        settings.updateNotificationLeadTime(15)
        settings.updateSimulatedTimeOffset(7_200_000L)
        val repository = repositoryWithTalk(scheduler, settings)
        repository.refreshSessions()
        repository.toggleFavorite("cur-1", true)
        scheduler.scheduledSessions.clear()

        repository.rescheduleAllFavorites()

        val scheduled = scheduler.scheduledSessions.single()
        assertEquals("cur-1", scheduled.sessionId)
        assertEquals(15, scheduled.leadTimeMinutes)
        assertEquals(7_200_000L, scheduled.timeOffsetMillis)
    }

    @Test
    fun reschedulingCancelsRemindersWhenNotificationsAreTurnedOff() = runTest {
        val scheduler = FakeReminderScheduler()
        val settings = settingsRepository()
        val repository = repositoryWithTalk(scheduler, settings)
        repository.refreshSessions()
        repository.toggleFavorite("cur-1", true)
        // refreshSessions() already armed both alarms while notifications were still on.
        scheduler.scheduledSessions.clear()
        scheduler.scheduledConferenceDone.clear()
        val conferenceDoneCancelsBefore = scheduler.conferenceDoneCancels

        settings.updateNotificationsEnabled(false)
        repository.rescheduleAllFavorites()

        assertTrue(scheduler.scheduledSessions.isEmpty())
        assertEquals(listOf("cur-1"), scheduler.cancelledSessionIds)
        assertTrue(scheduler.scheduledConferenceDone.isEmpty())
        assertEquals(conferenceDoneCancelsBefore + 1, scheduler.conferenceDoneCancels)
    }

    @Test
    fun onlyFavouritesAreScheduled() = runTest {
        val scheduler = FakeReminderScheduler()
        val settings = settingsRepository()
        val repository = repositoryWithTalk(scheduler, settings)
        repository.refreshSessions()

        repository.rescheduleAllFavorites()

        assertTrue(scheduler.scheduledSessions.isEmpty())
    }
}
