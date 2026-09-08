package com.olavbg.javazone.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.olavbg.javazone.JavaZoneConfig
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavDestination : NavKey {
    @Serializable
    data object Timeline : NavDestination

    @Serializable
    data class SessionDetail(
        val sessionId: String,
        val year: Int = JavaZoneConfig.CURRENT_YEAR,
    ) : NavDestination

    @Serializable
    data object Settings : NavDestination
}
