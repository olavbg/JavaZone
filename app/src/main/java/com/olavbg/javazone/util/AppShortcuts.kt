package com.olavbg.javazone.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.olavbg.javazone.MainActivity
import com.olavbg.javazone.R

object AppShortcuts {
    fun updateShortcuts(context: Context) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        val localizedContext = AppLocale.localizedContext(context)

        val favoritesIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.olavbg.javazone.action.FAVORITES"
            putExtra("shortcut", "favorites")
        }
        val favoritesShortcut = ShortcutInfo.Builder(context, "favorites")
            .setShortLabel(localizedContext.getString(R.string.shortcut_favorites_short))
            .setLongLabel(localizedContext.getString(R.string.shortcut_favorites_long))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_favorites))
            .setIntent(favoritesIntent)
            .setRank(0)
            .build()

        val settingsIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.olavbg.javazone.action.SETTINGS"
            putExtra("shortcut", "settings")
        }
        val settingsShortcut = ShortcutInfo.Builder(context, "settings")
            .setShortLabel(localizedContext.getString(R.string.shortcut_settings_short))
            .setLongLabel(localizedContext.getString(R.string.shortcut_settings_long))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_settings))
            .setIntent(settingsIntent)
            .setRank(1)
            .build()

        shortcutManager.dynamicShortcuts = listOf(favoritesShortcut, settingsShortcut)
    }
}
