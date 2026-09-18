package com.olavbg.javazone.locale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

class TranslationConsistencyTest {

    private fun readKeys(localeDir: String): Map<EntryKind, Set<String>> {
        val raw = File("src/main/res/$localeDir/strings.xml").readText()
        val names = Regex("""<(string|plurals)\s+name="([^"]+)"""").findAll(raw)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toList()
        return names.groupBy({ EntryKind.valueOf(it.first) }, { it.second })
            .mapValues { it.value.toSet() }
    }

    private enum class EntryKind { string, plurals }

    private fun pluralsQuantities(localeDir: String, key: String): Set<String> {
        val raw = File("src/main/res/$localeDir/strings.xml").readText()
        val block = Regex("""<plurals\s+name="$key">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .find(raw)?.groupValues?.get(1) ?: ""
        return Regex("""<item\s+quantity="([^"]+)"""").findAll(block)
            .map { it.groupValues[1] }
            .toSet()
    }

    @Test
    fun englishAndNorwegianHaveIdenticalKeySets() {
        val english = readKeys("values")
        for (localeDir in listOf("values-nb")) {
            val norwegian = readKeys(localeDir)
            assertEquals(
                "string keys differ for $localeDir",
                english[EntryKind.string],
                norwegian[EntryKind.string]
            )
            assertEquals(
                "plurals keys differ for $localeDir",
                english[EntryKind.plurals],
                norwegian[EntryKind.plurals]
            )
        }
    }

    @Test
    fun everyPluralsHasOneAndOtherQuantity_inAllLocales() {
        for (localeDir in listOf("values", "values-nb")) {
            val keys = readKeys(localeDir)[EntryKind.plurals] ?: error("no plurals for $localeDir")
            keys.forEach { key ->
                val quantities = pluralsQuantities(localeDir, key)
                assertTrue(
                    "plurals $key in $localeDir must define both 'one' and 'other'",
                    quantities.contains("one") && quantities.contains("other")
                )
            }
        }
    }

    @Test
    fun englishAndNorwegianDifferInValues_forTranslatedKeys() {
        val rawEn = File("src/main/res/values/strings.xml").readText()
        val rawNb = File("src/main/res/values-nb/strings.xml").readText()
        assertEquals("Norsk", Regex("""<string name="language_norwegian">([^<]*)</string>""")
            .find(rawNb)!!.groupValues[1])
        assertEquals("Norwegian", Regex("""<string name="language_norwegian">([^<]*)</string>""")
            .find(rawEn)!!.groupValues[1])
    }
}