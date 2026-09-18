package com.olavbg.javazone.locale

import com.olavbg.javazone.model.AppLanguage
import com.olavbg.javazone.util.formatDay
import com.olavbg.javazone.util.formatFullDay
import com.olavbg.javazone.util.localizedDayName
import com.olavbg.javazone.util.shortDayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class AppLanguageTest {

    @Test
    fun systemHasNoLocaleOverride() {
        assertEquals(AppLanguage.System, AppLanguage.fromStorage("system"))
        assertNull(AppLanguage.System.toLocale())
    }

    @Test
    fun storageValuesRoundTrip() {
        assertEquals(AppLanguage.Norwegian, AppLanguage.fromStorage("nb"))
        assertEquals(AppLanguage.English, AppLanguage.fromStorage("en"))
        assertEquals("nb", AppLanguage.Norwegian.storageValue)
        assertEquals("en", AppLanguage.English.storageValue)
    }

    @Test
    fun unknownOrNullStorageValueReturnsNull_andAppLocaleFallsBackToSystem() {
        // The System fallback lives in AppLocale.storedLanguage, so fromStorage itself is null.
        assertNull(AppLanguage.fromStorage(null))
        assertNull(AppLanguage.fromStorage("de"))
    }

    @Test
    fun norwegianTagIsCanonicalBokmal() {
        assertEquals("nb", AppLanguage.Norwegian.toLocale()?.toLanguageTag())
        assertEquals("en", AppLanguage.English.toLocale()?.toLanguageTag())
    }

    @Test
    fun englishLocaleIsSelected_forEnglishDayKeys() {
        val zulu = "2026-09-07T08:00:00Z"
        assertEquals("Monday", formatDay(zulu, Locale.ENGLISH))
        assertEquals("Monday", localizedDayName("Monday", Locale.ENGLISH))
    }

    @Test
    fun norwegianLocaleFormatsDaysInNorwegian() {
        val zulu = "2026-09-07T08:00:00Z"
        assertEquals("Mandag", formatDay(zulu, Locale.forLanguageTag("no")))
        assertEquals("Mandag", formatDay(zulu, Locale.forLanguageTag("nb")))
        assertEquals("Mandag", localizedDayName("Monday", Locale.forLanguageTag("nb")))
    }

    @Test
    fun fullDayFormatUsesLocaleSpecificPattern() {
        val zulu = "2026-09-07T08:00:00Z"
        assertEquals("Mon, Sep 7, 2026", formatFullDay(zulu, Locale.ENGLISH))
        assertEquals("man. 7. sep. 2026", formatFullDay(zulu, Locale.forLanguageTag("no")))
    }

    @Test
    fun shortDayNameRespectsLocale() {
        assertEquals("man.", shortDayName("Monday", Locale.forLanguageTag("no")))
        assertEquals("Mon", shortDayName("Monday", Locale.ENGLISH))
        assertEquals("unknown", shortDayName("unknown", Locale.ENGLISH))
    }
}