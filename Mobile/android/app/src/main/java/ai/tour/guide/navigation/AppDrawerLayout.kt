package ai.tour.guide.navigation

import ai.tour.guide.R
import ai.tour.guide.ui.screens.main.AccountSettingsScreen
import ai.tour.guide.ui.screens.main.AppSettingsScreen
import ai.tour.guide.ui.screens.main.DashboardScreen
import ai.tour.guide.ui.screens.main.ProfilePreferencesScreen
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Composable
fun AppDrawerLayout(modifier: Modifier = Modifier) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack = rememberNavBackStack(Route.Dashboard)
    val selectedRoute = backStack.lastOrNull() as? Route ?: Route.Dashboard

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
                TopBar(
                    selectedRoute = selectedRoute,
                    drawerState = drawerState,
                    backStack = backStack
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                NavDisplay(
                    modifier = Modifier.fillMaxSize(),
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    entryProvider = { key ->
                        when (key) {
                            is Route.Dashboard -> NavEntry(key) {
                                DashboardScreen()
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

                            else -> NavEntry(key) {
                                DashboardScreen()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DrawerContent(
    selectedRoute: Route,
    onRouteSelected: (Route) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = stringResource(R.string.navigation_drawer_header),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.titleLarge,
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.navigation_drawer_item_dashboard)) },
            selected = selectedRoute == Route.Dashboard,
            onClick = { onRouteSelected(Route.Dashboard) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null
                )
            }
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.navigation_drawer_item_profile)) },
            selected = selectedRoute == Route.Profile,
            onClick = { onRouteSelected(Route.Profile) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null
                )
            }
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.navigation_drawer_item_app_settings)) },
            selected = selectedRoute == Route.Settings,
            onClick = { onRouteSelected(Route.Settings) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null
                )
            }
        )
        HorizontalDivider(modifier = Modifier.padding(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(selectedRoute: Route, drawerState: DrawerState, backStack: NavBackStack<NavKey>) {
    val scope = rememberCoroutineScope()
    val routesWithBackButton = listOf(
        Route.AccountSettings
    )

    CenterAlignedTopAppBar(
        title = {
            Text(
                when (selectedRoute) {
                    Route.Dashboard -> stringResource(R.string.navigation_dashboard_route_title)
                    Route.Profile -> stringResource(R.string.navigation_profile_preferences_route_title)
                    Route.Settings -> stringResource(R.string.navigation_app_settings_route_title)
                    Route.AccountSettings -> stringResource(R.string.navigation_account_settings_route_title)
                }
            )
        },
        navigationIcon = {
            if (routesWithBackButton.contains(selectedRoute)) {
                IconButton(onClick = {
                    scope.launch {
                        backStack.removeLastOrNull()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.app_bar_hamburger_menu_content_description)
                    )
                }
            } else {
                IconButton(onClick = {
                    scope.launch {
                        if (drawerState.isClosed) {
                            drawerState.open()
                        } else {
                            drawerState.close()
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.app_bar_hamburger_menu_content_description)
                    )
                }
            }
        }
    )
}
