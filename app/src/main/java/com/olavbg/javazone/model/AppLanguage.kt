package com.olavbg.javazone.model

import java.util.Locale

/**
 * The app language chosen by the user in Settings.
 *
 * [System] follows the device language (no override is stored), while [Norwegian]
 * and [English] pin the app to that language regardless of the device locale.
 *
 * Norwegian is represented by the canonical Bokmål tag "nb"; the legacy "no"
 * tag aliases "nb" on Android, so a single `values-nb` resource directory
 * covers both.
 */
enum class AppLanguage(val storageValue: String) {
    System("system"),
    Norwegian("nb"),
    English("en");

    /** The [Locale] to force, or `null` when following the system language. */
    fun toLocale(): Locale? = when (this) {
        System -> null
        Norwegian -> Locale.forLanguageTag("nb")
        English -> Locale.forLanguageTag("en")
    }

    companion object {
        fun fromStorage(value: String?): AppLanguage? =
            entries.firstOrNull { it.storageValue == value }
    }
}