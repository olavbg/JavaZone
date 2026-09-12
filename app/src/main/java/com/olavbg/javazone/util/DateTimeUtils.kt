package com.olavbg.javazone.util

import com.olavbg.javazone.model.Session
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatTime(zulu: String): String {
    return try {
        val instant = Instant.parse(zulu)
        val dateTime = instant.atZone(ZoneId.of("Europe/Oslo"))
        dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        ""
    }
}

fun formatDay(zulu: String): String {
    return try {
        val instant = Instant.parse(zulu)
        val dateTime = instant.atZone(ZoneId.of("Europe/Oslo"))
        dateTime.format(DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("no")))
            .replaceFirstChar { it.uppercase() }
    } catch (_: Exception) {
        ""
    }
}

fun formatFullDay(zulu: String): String? {
    return try {
        val instant = Instant.parse(zulu)
        val dateTime = instant.atZone(ZoneId.of("Europe/Oslo"))
        dateTime.format(DateTimeFormatter.ofPattern("EEE d. MMM yyyy", Locale.forLanguageTag("no")))
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
    return try {
        val start = Instant.parse(session.startTimeZulu)
        val end = Instant.parse(session.endTimeZulu)
        val duration = Duration.between(start, end).toMinutes()
        if (duration <= 0) 60 else duration
    } catch (e: Exception) {
        60
    }
}
