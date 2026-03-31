package ai.tour.guide.navigation

import ai.tour.guide.ui.screens.main.AppSettingsScreen
import ai.tour.guide.ui.screens.main.DashboardScreen
import ai.tour.guide.ui.screens.main.ProfilePreferencesScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay

@Composable
fun NavigationRoot(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Route.Dashboard)
    NavDisplay(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                is Route.Dashboard -> NavEntry(key) {
                    DashboardScreen()
                }
                is Route.Profile -> NavEntry(key) {
                    ProfilePreferencesScreen()
                }
                is Route.Settings -> NavEntry(key) {
                    AppSettingsScreen()
                }
                else -> error("Invalid NavKey: $key")
            }
        }
    )
}
