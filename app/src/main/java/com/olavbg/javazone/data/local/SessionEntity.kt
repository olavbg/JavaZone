package com.olavbg.javazone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.olavbg.javazone.model.Speaker

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val abstractText: String,
    val room: String,
    val startTimeZulu: String,
    val endTimeZulu: String,
    val format: String,
    val language: String?,
    val videoUrl: String?,
    val speakers: List<Speaker>
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val sessionId: String
)
