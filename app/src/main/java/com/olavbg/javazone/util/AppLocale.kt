package com.olavbg.javazone.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.data.repository.dataStore
import com.olavbg.javazone.model.AppLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * Applies the persisted [AppLanguage] override to the running app.
 *
 * Two mechanisms are used, mirroring how the platform expects per-app-language
 * preferences to behave:
 *
 * - On API 33+ the system's [LocaleManager] owns the locale. Changing it makes
 *   the OS recreate the activity with the new configuration, so both the app's
 *   resources and `Locale.getDefault()` follow automatically.
 * - Below API 33 the override is re-applied by wrapping [Context]s with the
 *   stored locale ([localizedContext]); callers trigger a recreation so the
 *   new configuration is picked up.
 */
object AppLocale {

    /** Reads the persisted override, falling back to [AppLanguage.System]. */
    private fun storedLanguage(context: Context): AppLanguage {
        val raw = runCatching {
            runBlocking { context.dataStore.data.first()[SettingsRepository.APP_LANGUAGE_KEY] }
        }.getOrNull()
        return AppLanguage.fromStorage(raw) ?: AppLanguage.System
    }

    /**
     * Returns [context] with the persisted language override applied to its
     * resources. Returns the context untouched when no override is set.
     * Used on API &lt; 33 where the system per-app-language support is unavailable.
     */
    fun localizedContext(context: Context): Context {
        val locale = storedLanguage(context).toLocale() ?: return context
        Locale.setDefault(locale)
        return context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(locale) }
        )
    }

    /**
     * Makes [language] take effect immediately (it must already be persisted).
     * On API 33+ the system recreates the activity; below that the activity is
     * recreated manually so its [localizedContext] wrapping picks up the change.
     */
    fun apply(context: Context, language: AppLanguage) {
        setDefaultLocale(language)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            localeManager.applicationLocales = language.toLocale()
                ?.let { LocaleList.forLanguageTags(it.toLanguageTag()) }
                ?: LocaleList.getEmptyLocaleList()
        } else {
            (context as? Activity)?.recreate()
        }
    }

    /** The device's own locale, unaffected by any in-app override. */
    fun systemLocale(): Locale {
        val systemLocales = runCatching { Resources.getSystem().configuration.locales }
            .getOrNull()
            ?.takeIf { !it.isEmpty }
        return systemLocales?.get(0) ?: Locale.getDefault()
    }

    private fun setDefaultLocale(language: AppLanguage) {
        Locale.setDefault(language.toLocale() ?: systemLocale())
    }
}