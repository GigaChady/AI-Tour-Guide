package ai.tour.guide.ui.navigation.wrappers

import ai.tour.guide.ui.navigation.DrawerContent
import ai.tour.guide.ui.navigation.Route
import ai.tour.guide.ui.navigation.topbars.DrawerLayoutTopBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Composable
fun WithDrawerLayout(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey> = rememberNavBackStack(),
    routeTitle: String = "",
    content: @Composable () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val selectedRoute = backStack.lastOrNull() as? Route

    ModalNavigationDrawer(
        modifier = modifier,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(0.75F)
                    .fillMaxHeight()
                    .safeDrawingPadding()
            ) {
                DrawerContent(
                    selectedRoute = selectedRoute,
                    onRouteSelected = { route ->
                        if (route != selectedRoute) {
                            Snapshot.withMutableSnapshot {
                                backStack.clear()
                                backStack.add(route)
                            }
                        }
                        scope.launch { drawerState.close() }
                    }
                )
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                DrawerLayoutTopBar(
                    drawerState = drawerState,
                    routeTitle = routeTitle
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
}