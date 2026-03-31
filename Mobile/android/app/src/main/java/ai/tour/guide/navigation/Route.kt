package ai.tour.guide.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {
    @Serializable
    data object Dashboard : Route, NavKey

    @Serializable
    data object Profile : Route, NavKey

    @Serializable
    data object Settings : Route, NavKey
}
