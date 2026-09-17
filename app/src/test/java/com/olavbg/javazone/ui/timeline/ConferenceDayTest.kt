package com.olavbg.javazone.ui.timeline

import com.olavbg.javazone.model.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ConferenceDayTest {

    private fun session(id: String, startZulu: String) = Session(
        id = id,
        title = "",
        abstract = "",
        room = "",
        startTimeZulu = startZulu,
        endTimeZulu = startZulu,
        format = "",
        language = null,
        videoUrl = null,
        speakers = emptyList()
    )

    private val sessions = listOf(
        session("1", "2026-09-01T08:00:00Z"),
        session("2", "2026-09-02T08:00:00Z"),
        session("3", "2026-09-03T08:00:00Z")
    )

    @Test
    fun returnsDayName_whenDateMatchesASessionDate() {
        assertEquals("Thursday", conferenceDayForDate(sessions, LocalDate.of(2026, 9, 3)))
    }

    @Test
    fun returnsNull_whenNoSessionOnDate_evenIfWeekdayNameMatches() {
        // 2026-09-17 is a Thursday, same weekday as the last conference day (2026-09-03),
        // but not a conference date. The old weekday-name match wrongly selected the last day.
        assertNull(conferenceDayForDate(sessions, LocalDate.of(2026, 9, 17)))
    }

    @Test
    fun returnsNull_forEmptyList() {
        assertNull(conferenceDayForDate(emptyList(), LocalDate.of(2026, 9, 1)))
    }
}
