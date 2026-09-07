package com.olavbg.javazone.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SessionsResponseDto(
    @Json(name = "sessions") val sessions: List<SessionDto>
)

@JsonClass(generateAdapter = true)
data class SessionDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String?,
    @Json(name = "abstract") val abstract: String?,
    @Json(name = "room") val room: String?,
    @Json(name = "startTimeZulu") val startTimeZulu: String?,
    @Json(name = "endTimeZulu") val endTimeZulu: String?,
    @Json(name = "format") val format: String?,
    @Json(name = "language") val language: String?,
    @Json(name = "video") val videoUrl: String?,
    @Json(name = "speakers") val speakers: List<SpeakerDto>?
)

@JsonClass(generateAdapter = true)
data class SpeakerDto(
    @Json(name = "name") val name: String,
    @Json(name = "bio") val bio: String?,
    @Json(name = "twitter") val twitter: String?
)
