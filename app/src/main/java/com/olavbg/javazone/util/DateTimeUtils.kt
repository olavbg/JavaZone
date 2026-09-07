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
