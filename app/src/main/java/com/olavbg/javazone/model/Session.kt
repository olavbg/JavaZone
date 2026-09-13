package com.olavbg.javazone.model

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class Session(
    val id: String,
    val title: String,
    val abstract: String,
    val room: String,
    val startTimeZulu: String,
    val endTimeZulu: String,
    val format: String,
    val language: String?,
    val videoUrl: String?,
    val speakers: List<Speaker>,
    val isFavorite: Boolean = false,
    // Parsed once at construction, so downstream code (grouping, per-card
    // live/remaining-time math) never has to re-parse the Zulu strings.
    val start: Instant? = parseOrNull(startTimeZulu),
    val end: Instant? = parseOrNull(endTimeZulu)
)

@Immutable
data class Speaker(
    val name: String,
    val bio: String?,
    val twitter: String?
)

private fun parseOrNull(value: String): Instant? =
    runCatching { Instant.parse(value) }.getOrNull()
