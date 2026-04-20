package ai.tour.guide.ui.navigation.wrappers

import ai.tour.guide.ui.navigation.topbars.ScreenTopBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

@Composable
fun WithScreenTopBar(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey> = rememberNavBackStack(),
    topBarActions: @Composable () -> Unit = {},
    hasBackButton: Boolean = false,
    routeTitle: String = "",
    content: @Composable () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenTopBar(
                backStack = backStack,
                hasBackButton = hasBackButton,
                routeTitle = routeTitle,
                actions = topBarActions
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}