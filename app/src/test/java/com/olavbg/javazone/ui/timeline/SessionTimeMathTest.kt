package com.olavbg.javazone.ui.timeline

import com.olavbg.javazone.model.Session
import com.olavbg.javazone.model.Speaker
import com.olavbg.javazone.util.isSessionActive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SessionTimeMathTest {

    private val start: Instant = Instant.parse("2026-09-22T10:00:00Z")
    private val end: Instant = Instant.parse("2026-09-22T10:50:00Z")

    private fun session(startIso: String, endIso: String) = Session(
        id = "s",
        title = "T",
        abstract = "",
        room = "R",
        startTimeZulu = startIso,
        endTimeZulu = endIso,
        format = "Presentation",
        language = "no",
        videoUrl = null,
        speakers = listOf(Speaker(name = "A", bio = null, twitter = null)),
        isFavorite = false
    )

    private val talk = session(start.toString(), end.toString())

    @Test
    fun progressIsZeroBeforeTheTalkStarts() {
        assertEquals(0f, calculateSessionProgress(talk, start.minusSeconds(60)), 0.001f)
    }

    @Test
    fun progressIsHalfwayAtTheMidpoint() {
        assertEquals(0.5f, calculateSessionProgress(talk, start.plusSeconds(25 * 60)), 0.001f)
    }

    @Test
    fun progressReachesOneAtTheEndAndStaysThere() {
        assertEquals(1f, calculateSessionProgress(talk, end), 0.001f)
        assertEquals(1f, calculateSessionProgress(talk, end.plusSeconds(600)), 0.001f)
    }

    @Test
    fun progressIsZeroForUnparseableTimes() {
        val broken = session("not-a-timestamp", "also-not-a-timestamp")
        assertEquals(0f, calculateSessionProgress(broken, start), 0.001f)
    }

    @Test
    fun progressIsZeroForAZeroLengthTalk() {
        val instant = start.toString()
        val empty = session(instant, instant)
        assertEquals(0f, calculateSessionProgress(empty, start), 0.001f)
    }

    @Test
    fun remainingMinutesCountsDownAndFloorsAtZero() {
        assertEquals(20L, calculateRemainingMinutes(talk, start.plusSeconds(30 * 60)))
        assertEquals(50L, calculateRemainingMinutes(talk, start))
        assertEquals(0L, calculateRemainingMinutes(talk, end.plusSeconds(60)))
    }

    @Test
    fun remainingMinutesIsZeroForUnparseableTimes() {
        val broken = session("nope", "nope")
        assertEquals(0L, calculateRemainingMinutes(broken, start))
    }

    @Test
    fun minutesUntilStartCountsDownAndFloorsAtZero() {
        assertEquals(30L, calculateMinutesUntilStart(talk, start.minusSeconds(30 * 60)))
        assertEquals(0L, calculateMinutesUntilStart(talk, start))
        assertEquals(0L, calculateMinutesUntilStart(talk, start.plusSeconds(600)))
    }

    @Test
    fun minutesUntilStartIsZeroForUnparseableTimes() {
        val broken = session("nope", "nope")
        assertEquals(0L, calculateMinutesUntilStart(broken, start))
    }

    @Test
    fun isPastOnlyOnceTheEndHasPassed() {
        assertFalse(isSessionPast(talk, start))
        assertFalse(isSessionPast(talk, end))
        assertTrue(isSessionPast(talk, end.plusSeconds(1)))
    }

    @Test
    fun isPastIsFalseWhenTheEndIsUnparseable() {
        val broken = session(start.toString(), "nope")
        assertFalse(isSessionPast(broken, end.plusSeconds(600)))
    }

    @Test
    fun isActiveIncludesTheStartInstantAndExcludesTheEndInstant() {
        assertTrue(isSessionActive(talk, start))
        assertTrue(isSessionActive(talk, start.plusSeconds(1)))
        assertTrue(isSessionActive(talk, end.minusSeconds(1)))
        assertFalse(isSessionActive(talk, end))
        assertFalse(isSessionActive(talk, start.minusSeconds(1)))
    }

    @Test
    fun isActiveIsFalseWhenEitherTimeIsUnparseable() {
        assertFalse(isSessionActive(session("nope", end.toString()), start))
        assertFalse(isSessionActive(session(start.toString(), "nope"), start))
    }
}
