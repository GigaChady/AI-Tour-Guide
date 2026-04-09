package ai.tour.guide.ui.navigation

import ai.tour.guide.ui.screens.main.AccountSettingsScreen
import ai.tour.guide.ui.screens.main.AppSettingsScreen
import ai.tour.guide.ui.screens.main.DashboardScreen
import ai.tour.guide.ui.screens.main.MapUserPositionScreen
import ai.tour.guide.ui.screens.main.ProfilePreferencesScreen
import ai.tour.guide.ui.screens.main.TourAudioPlayerScreen
import ai.tour.guide.ui.screens.main.TripEndSummaryScreen
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay

@Composable
fun AppNavigationDisplay(modifier: Modifier = Modifier, backStack: NavBackStack<NavKey>) {
    NavDisplay(
        modifier = modifier.fillMaxSize(),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        entryProvider = { key ->
            when (key) {
                is Route.Dashboard -> NavEntry(key) {
                    DashboardScreen(backStack = backStack)
                }

                is Route.Profile -> NavEntry(key) {
                    ProfilePreferencesScreen(backStack = backStack)
                }

                is Route.Settings -> NavEntry(key) {
                    AppSettingsScreen()
                }

                is Route.AccountSettings -> NavEntry(key) {
                    AccountSettingsScreen()
                }

                is Route.TourAudioPlayer -> NavEntry(key) {
                    TourAudioPlayerScreen(backStack = backStack)
                }

                is Route.MapUserPosition -> NavEntry(key) {
                    MapUserPositionScreen()
                }

                is Route.TripEndSummary -> NavEntry(key) {
                    TripEndSummaryScreen(backStack = backStack)
                }

                else -> NavEntry(key) {
                    DashboardScreen(backStack = backStack)
                }
            }
        }
    )
}