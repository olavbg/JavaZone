package com.olavbg.javazone.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface SleepingPillApi {
    @GET("allSessions")
    suspend fun getConferences(): ConferencesResponseDto

    @GET("allSessions/{conferenceId}")
    suspend fun getSessions(@Path("conferenceId") conferenceId: String): SessionsResponseDto

    companion object {
        const val BASE_URL = "https://sleepingpill.javazone.no/public/"
    }
}