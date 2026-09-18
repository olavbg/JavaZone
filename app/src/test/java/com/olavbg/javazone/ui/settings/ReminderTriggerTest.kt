package com.olavbg.javazone.ui.settings

import com.olavbg.javazone.model.Session
import com.olavbg.javazone.model.Speaker
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTriggerTest {

    private fun session(id: String, startIso: String, isFavorite: Boolean = true) = Session(
        id = id,
        title = "Title",
        abstract = "",
        room = "Room",
        startTimeZulu = startIso,
        endTimeZulu = startIso,
        format = "Foredrag",
        language = "no",
        videoUrl = null,
        speakers = listOf(Speaker(name = "A", bio = null, twitter = null)),
        isFavorite = isFavorite
    )

    private val now = 1_800_000_000_000L

    @Test
    fun favoriteWhoseLeadTimeWindowHasElapsedCountsAsMissed() {
        val start = Instant.ofEpochMilli(now + 10 * 60_000L).toString()
        assertEquals(1, countMissedReminders(listOf(session("a", start)), emptySet(), 0L, 10, now))
    }

    @Test
    fun favoriteWhoseLeadTimeIsStillAheadIsNotCounted() {
        val start = Instant.ofEpochMilli(now + 11 * 60_000L).toString()
        assertEquals(0, countMissedReminders(listOf(session("a", start)), emptySet(), 0L, 10, now))
    }

    @Test
    fun nonFavoriteSessionsAreNotCounted() {
        val start = Instant.ofEpochMilli(now - 60_000L).toString()
        assertEquals(
            0,
            countMissedReminders(listOf(session("a", start, isFavorite = false)), emptySet(), 0L, 10, now)
        )
    }

    @Test
    fun simulatedFutureTimeCanMarkPastStartAsMissed() {
        val start = Instant.ofEpochMilli(now + 2 * 3_600_000L).toString()
        assertEquals(1, countMissedReminders(listOf(session("a", start)), emptySet(), 2 * 3_600_000L, 10, now))
    }

    @Test
    fun alreadyFiredSessionIsNotCountedAsMissed() {
        val start = Instant.ofEpochMilli(now - 60_000L).toString()
        assertEquals(0, countMissedReminders(listOf(session("a", start)), setOf("a"), 0L, 10, now))
    }
}