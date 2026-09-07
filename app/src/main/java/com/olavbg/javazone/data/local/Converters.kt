package com.olavbg.javazone.data.local

import androidx.room.TypeConverter
import com.olavbg.javazone.model.Speaker
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val speakerListType = Types.newParameterizedType(List::class.java, Speaker::class.java)
    private val adapter = moshi.adapter<List<Speaker>>(speakerListType)

    @TypeConverter
    fun fromSpeakerList(speakers: List<Speaker>?): String? {
        return adapter.toJson(speakers)
    }

    @TypeConverter
    fun toSpeakerList(speakersJson: String?): List<Speaker>? {
        return speakersJson?.let { adapter.fromJson(it) }
    }
}
