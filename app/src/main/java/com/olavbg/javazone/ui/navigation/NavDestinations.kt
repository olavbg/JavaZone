package com.olavbg.javazone.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavDestination : NavKey {
    @Serializable
    data object Timeline : NavDestination

    @Serializable
    data class SessionDetail(val sessionId: String) : NavDestination

    @Serializable
    data object Archive : NavDestination

    @Serializable
    data object Settings : NavDestination
}
