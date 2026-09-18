package com.olavbg.javazone.util

import com.olavbg.javazone.model.Session
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ZoneId is immutable and thread-safe; creating it once avoids expensive object
// allocation on every formatting call (called per list item).
private val OSLO_ZONE: ZoneId = ZoneId.of("Europe/Oslo")
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// The day pattern differs between Norwegian ("mandag 7. sep") and English
// ("Mon, Sep 7"), and formatters are locale-specific, so they are built per
// locale rather than cached globally.
private fun dayFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE", locale)

private fun fullDayFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern(
        if (locale.language == "no" || locale.language == "nb" || locale.language == "nn") {
            "EEE d. MMM yyyy"
        } else {
            "EEE, MMM d, yyyy"
        },
        locale
    )

fun formatTime(zulu: String): String {
    return try {
        val instant = Instant.parse(zulu)
        instant.atZone(OSLO_ZONE).format(TIME_FORMATTER)
    } catch (_: Exception) {
        ""
    }
}

/** Formats an already-parsed session time, avoiding a redundant [Instant.parse]. */
fun formatTime(instant: Instant?): String {
    return try {
        instant?.atZone(OSLO_ZONE)?.format(TIME_FORMATTER) ?: ""
    } catch (_: Exception) {
        ""
    }
}

fun formatDay(zulu: String, locale: Locale = Locale.getDefault()): String {
    return try {
        val instant = Instant.parse(zulu)
        instant.atZone(OSLO_ZONE).format(dayFormatter(locale))
            .replaceFirstChar { it.uppercase() }
    } catch (_: Exception) {
        ""
    }
}

fun formatFullDay(zulu: String, locale: Locale = Locale.getDefault()): String? {
    return try {
        val instant = Instant.parse(zulu)
        instant.atZone(OSLO_ZONE).format(fullDayFormatter(locale))
    } catch (_: Exception) {
        null
    }
}

private val NORWEGIAN_DAYS: Map<String, String> = mapOf(
    "Monday" to "Mandag",
    "Tuesday" to "Tirsdag",
    "Wednesday" to "Onsdag",
    "Thursday" to "Torsdag",
    "Friday" to "Fredag",
    "Saturday" to "Lørdag",
    "Sunday" to "Søndag"
)

private val NORWEGIAN_SHORT_DAYS: Map<String, String> = mapOf(
    "Monday" to "man.",
    "Tuesday" to "tir.",
    "Wednesday" to "ons.",
    "Thursday" to "tor.",
    "Friday" to "fre.",
    "Saturday" to "lør.",
    "Sunday" to "søn."
)

private val ENGLISH_SHORT_DAYS: Map<String, String> = mapOf(
    "Monday" to "Mon",
    "Tuesday" to "Tue",
    "Wednesday" to "Wed",
    "Thursday" to "Thu",
    "Friday" to "Fri",
    "Saturday" to "Sat",
    "Sunday" to "Sun"
)

private fun isNorwegian(locale: Locale): Boolean =
    locale.language == "no" || locale.language == "nb" || locale.language == "nn"

/** Localizes a weekday key coming from the API (always an English day name). */
fun localizedDayName(dayKey: String, locale: Locale = Locale.getDefault()): String = when {
    isNorwegian(locale) -> NORWEGIAN_DAYS[dayKey] ?: dayKey
    else -> dayKey
}

fun shortDayName(dayKey: String, locale: Locale = Locale.getDefault()): String = when {
    isNorwegian(locale) -> NORWEGIAN_SHORT_DAYS[dayKey] ?: dayKey
    else -> ENGLISH_SHORT_DAYS[dayKey] ?: dayKey
}

fun calculateSessionDurationMinutes(session: Session): Long {
    val start = session.start
    val end = session.end
    return if (start != null && end != null) {
        val duration = Duration.between(start, end).toMinutes()
        if (duration <= 0) 60 else duration
    } else {
        60
    }
}

fun isSessionActive(session: Session, currentTime: Instant): Boolean {
    val start = session.start ?: return false
    val end = session.end ?: return false
    return (currentTime.isAfter(start) || currentTime == start) && currentTime.isBefore(end)
}

fun extractRoomNumber(room: String): Int {
    val digits = room.filter { it.isDigit() }
    return digits.toIntOrNull() ?: Int.MAX_VALUE
}
