package com.olavbg.javazone.model

/**
 * How the app resolves its light/dark theme.
 *
 * [Dark] and [Light] force the theme regardless of the device, while [System]
 * follows the device's current theme. The default is [Dark], so the app no longer
 * follows the phone's theme unless the user explicitly chooses [System].
 */
enum class ThemeMode(val storageValue: String) {
    Dark("dark"),
    Light("light"),
    System("system");

    /** Resolves this mode to a dark/light boolean given the current system theme. */
    fun resolveDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        Dark -> true
        Light -> false
        System -> systemInDarkTheme
    }

    companion object {
        fun fromStorage(value: String?): ThemeMode? =
            entries.firstOrNull { it.storageValue == value }
    }
}