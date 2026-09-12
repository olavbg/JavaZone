package com.olavbg.javazone.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConferencesResponseDto(
    @Json(name = "conferences") val conferences: List<ConferenceDto>
)

@JsonClass(generateAdapter = true)
data class ConferenceDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String?,
    @Json(name = "slug") val slug: String?
)