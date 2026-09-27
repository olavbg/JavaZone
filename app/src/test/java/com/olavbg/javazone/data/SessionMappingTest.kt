package com.olavbg.javazone.data

import com.olavbg.javazone.data.local.Converters
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.model.Session
import com.olavbg.javazone.model.Speaker
import com.olavbg.javazone.support.FakeSessionDao
import com.olavbg.javazone.support.FakeSleepingPillApi
import com.olavbg.javazone.support.TimeoutRule
import com.olavbg.javazone.support.testSessionDto
import com.olavbg.javazone.support.testSpeaker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Covers the DTO -> Room entity -> domain hop, including the null-tolerant parsing the whole
 * UI branches on (`Session.start` / `Session.end`).
 */
class SessionMappingTest {

    @get:Rule
    val timeoutRule = TimeoutRule()

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
        speakers = emptyList(),
        isFavorite = false
    )

    @Test
    fun parsesZuluTimesOnceAtConstruction() {
        val parsed = session("2026-09-22T10:00:00Z", "2026-09-22T10:50:00Z")
        assertEquals(Instant.parse("2026-09-22T10:00:00Z"), parsed.start)
        assertEquals(Instant.parse("2026-09-22T10:50:00Z"), parsed.end)
    }

    @Test
    fun unparseableTimesBecomeNullInsteadOfThrowing() {
        val broken = session("not-a-time", "also-not-a-time")
        assertNull(broken.start)
        assertNull(broken.end)
    }

    @Test
    fun nullTimesAreTreatedTheSameAsUnparseableOnes() {
        val empty = session("", "")
        assertNull(empty.start)
        assertNull(empty.end)
    }

    @Test
    fun speakerListSurvivesAStorageRoundTrip() {
        val speakers = listOf(
            Speaker("Ada", "bio", "@ada", bluesky = "ada", linkedin = "ada", pictureUrl = "p")
        )
        val converters = Converters()
        assertEquals(speakers, converters.toSpeakerList(converters.fromSpeakerList(speakers)))
    }

    @Test
    fun emptyAndNullSpeakerListsRoundTripUnchanged() {
        val converters = Converters()

        assertEquals(emptyList<Speaker>(), converters.toSpeakerList(converters.fromSpeakerList(emptyList())))
        assertNull(converters.toSpeakerList(null))
        // Moshi encodes a null list as the literal "null" rather than a SQL NULL. It decodes
        // back to null, and Room never routes a null through the converter anyway, since
        // SessionEntity.speakers is non-null.
        assertEquals("null", converters.fromSpeakerList(null))
        assertNull(converters.toSpeakerList(converters.fromSpeakerList(null)))
    }

    @Test
    fun missingOptionalFieldsFallBackToSafeDefaults() = runTest {
        val api = FakeSleepingPillApi(
            currentYearSessions = listOf(
                testSessionDto(
                    id = "cur-1",
                    title = "Uten valgfrie felter",
                    abstract = null,
                    room = null,
                    format = null,
                    language = null,
                    startTimeZulu = "2026-09-22T10:00:00Z"
                )
            )
        )
        val repository = SessionRepository(api, FakeSessionDao())

        repository.refreshSessions()

        val mapped = repository.getSessions().first().single()
        assertEquals("Uten valgfrie felter", mapped.title)
        assertEquals("", mapped.abstract)
        assertEquals("", mapped.room)
        assertEquals("", mapped.format)
        assertEquals(emptyList<Speaker>(), mapped.speakers)
        assertNull(mapped.language)
    }

    @Test
    fun anUnparseableStartTimeSurvivesTheMappingAsNull() = runTest {
        val api = FakeSleepingPillApi(
            currentYearSessions = listOf(testSessionDto(id = "cur-1", startTimeZulu = "garbage"))
        )
        val repository = SessionRepository(api, FakeSessionDao())

        repository.refreshSessions()

        val mapped = repository.getSessions().first().single()
        assertNull(mapped.start)
        assertNull(mapped.end)
    }

    @Test
    fun optionalDetailFieldsSurviveTheMapping() = runTest {
        val api = FakeSleepingPillApi(
            currentYearSessions = listOf(
                testSessionDto(
                    id = "cur-1",
                    startTimeZulu = "2026-09-22T10:00:00Z",
                    endTimeZulu = "2026-09-22T10:50:00Z",
                    videoUrl = "https://vimeo.com/1",
                    intendedAudience = "Devs",
                    suggestedKeywords = "kotlin",
                    speakers = listOf(testSpeaker("Ada"))
                )
            )
        )
        val repository = SessionRepository(api, FakeSessionDao())

        repository.refreshSessions()

        val mapped = repository.getSessions().first().single()
        assertEquals("https://vimeo.com/1", mapped.videoUrl)
        assertEquals("Devs", mapped.intendedAudience)
        assertEquals("kotlin", mapped.suggestedKeywords)
        assertEquals("Ada", mapped.speakers.single().name)
        assertTrue(mapped.start != null)
    }
}
