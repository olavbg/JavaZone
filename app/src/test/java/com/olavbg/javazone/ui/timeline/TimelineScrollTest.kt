package com.olavbg.javazone.ui.timeline

import com.olavbg.javazone.model.Session
import com.olavbg.javazone.model.Speaker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TimelineScrollTest {

    private fun session(id: String, startIso: String, endIso: String = startIso) = Session(
        id = id,
        title = id,
        abstract = "",
        room = "Room 1",
        startTimeZulu = startIso,
        endTimeZulu = endIso,
        format = "Presentation",
        language = "no",
        videoUrl = null,
        speakers = emptyList(),
        isFavorite = false
    )

    private fun group(key: String, vararg sessions: Session) =
        AgendaGroup(key = key, headerLabel = key, sessions = sessions.toList())

    private val first = session("a", "2026-09-22T10:00:00Z", "2026-09-22T10:50:00Z")
    private val second = session("b", "2026-09-22T11:00:00Z", "2026-09-22T11:50:00Z")
    private val third = session("c", "2026-09-22T12:00:00Z", "2026-09-22T12:50:00Z")

    // One header item plus one item per session.
    private val agenda = listOf(
        group("10:00", first),
        group("11:00", second),
        group("12:00", third)
    )

    /** Mirrors the list layout: one header item, then one item per session, for each group. */
    private fun groupAtItemIndex(groups: List<AgendaGroup>, itemIndex: Int): AgendaGroup {
        var offset = 0
        for (group in groups) {
            val itemCount = 1 + group.sessions.size
            if (itemIndex < offset + itemCount) return group
            offset += itemCount
        }
        throw AssertionError("no group at item index $itemIndex")
    }

    @Test
    fun findsTheActiveGroupEvenWhenAnEarlierGroupIsStillUpcoming() {
        val index = findFirstActiveOrUpcomingIndex(agenda, Instant.parse("2026-09-22T11:10:00Z"))
        assertEquals(2, index)
    }

    @Test
    fun fallsBackToTheFirstUpcomingGroupWhenNothingIsActive() {
        val index = findFirstActiveOrUpcomingIndex(agenda, Instant.parse("2026-09-22T09:00:00Z"))
        assertEquals(0, index)
    }

    @Test
    fun returnsNullOnceEveryGroupIsInThePast() {
        val index = findFirstActiveOrUpcomingIndex(agenda, Instant.parse("2026-09-22T20:00:00Z"))
        assertNull(index)
    }

    @Test
    fun returnsNullForAnEmptyAgenda() {
        assertNull(findFirstActiveOrUpcomingIndex(emptyList(), Instant.now()))
    }

    @Test
    fun indexPointsAtTheHeaderOfTheTargetGroup() {
        val index = findFirstActiveOrUpcomingIndex(agenda, Instant.parse("2026-09-22T12:10:00Z"))!!
        // Three single-session groups occupy items 0-1, 2-3 and 4-5, so the third
        // group's header is item 4.
        assertEquals(4, index)
        assertEquals("12:00", groupAtItemIndex(agenda, index).headerLabel)
    }

    @Test
    fun indexAccountsForGroupsHoldingSeveralSessions() {
        val multi = listOf(
            group("10:00", first, second),
            group("12:00", third)
        )
        assertEquals(3, findFirstActiveOrUpcomingIndex(multi, Instant.parse("2026-09-22T12:10:00Z")))
    }

    @Test
    fun aGroupCountsAsActiveWhenAnyOfItsSessionsIsActive() {
        val past = session("a", "2026-09-22T09:00:00Z", "2026-09-22T09:30:00Z")
        val active = session("d", "2026-09-22T10:00:00Z", "2026-09-22T11:00:00Z")
        val overlapping = listOf(group("09:00", past, active))
        // Only the second session is live; an implementation that inspected just the first
        // session would find nothing active and no future group, and return null.
        assertEquals(0, findFirstActiveOrUpcomingIndex(overlapping, Instant.parse("2026-09-22T10:06:00Z")))
    }

    @Test
    fun groupIsConsideredForADayWhenItsFirstSessionMatches() {
        assertTrue(isGroupedSessionsForDay(agenda, "Tuesday"))
        assertTrue(isGroupedSessionsForDay(agenda, "tuesday"))
    }

    @Test
    fun groupIsConsideredForADayOnlyWhenSelectedDayIsNull() {
        assertTrue(isGroupedSessionsForDay(agenda, null))
    }

    @Test
    fun groupIsNotConsideredForAnotherDay() {
        assertFalse(isGroupedSessionsForDay(agenda, "Wednesday"))
    }

    @Test
    fun emptyAgendaIsNeverConsideredForADay() {
        assertFalse(isGroupedSessionsForDay(emptyList(), null))
        assertFalse(isGroupedSessionsForDay(emptyList(), "Tuesday"))
    }

    @Test
    fun dayWithNoSessionsAtAllIsNotConsideredForADay() {
        val noSessions = listOf(group("10:00"))
        assertFalse(isGroupedSessionsForDay(noSessions, "Tuesday"))
    }
}
