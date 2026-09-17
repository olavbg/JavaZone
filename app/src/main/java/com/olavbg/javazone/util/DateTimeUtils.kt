package com.olavbg.javazone.util

import com.olavbg.javazone.model.Session
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// DateTimeFormatter and ZoneId are immutable and thread-safe; creating them once
// avoids expensive object allocation on every formatting call (called per list item).
private val OSLO_ZONE: ZoneId = ZoneId.of("Europe/Oslo")
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("no"))
private val FULL_DAY_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d. MMM yyyy", Locale.forLanguageTag("no"))

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

fun formatDay(zulu: String): String {
    return try {
        val instant = Instant.parse(zulu)
        instant.atZone(OSLO_ZONE).format(DAY_FORMATTER)
            .replaceFirstChar { it.uppercase() }
    } catch (_: Exception) {
        ""
    }
}

fun formatFullDay(zulu: String): String? {
    return try {
        val instant = Instant.parse(zulu)
        instant.atZone(OSLO_ZONE).format(FULL_DAY_FORMATTER)
    } catch (_: Exception) {
        null
    }
}

fun localizedDayName(dayKey: String): String = when (dayKey) {
    "Monday" -> "Mandag"
    "Tuesday" -> "Tirsdag"
    "Wednesday" -> "Onsdag"
    "Thursday" -> "Torsdag"
    "Friday" -> "Fredag"
    "Saturday" -> "Lørdag"
    "Sunday" -> "Søndag"
    else -> dayKey
}

fun shortDayName(dayKey: String): String = when (dayKey) {
    "Monday" -> "man."
    "Tuesday" -> "tir."
    "Wednesday" -> "ons."
    "Thursday" -> "tor."
    "Friday" -> "fre."
    "Saturday" -> "lør."
    "Sunday" -> "søn."
    else -> dayKey
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
