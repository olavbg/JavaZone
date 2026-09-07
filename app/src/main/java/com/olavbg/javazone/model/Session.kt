package com.olavbg.javazone.model

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
    val isFavorite: Boolean = false
)

data class Speaker(
    val name: String,
    val bio: String?,
    val twitter: String?
)
