package ai.tour.guide.ui.components.navigation

import ai.tour.guide.R
import ai.tour.guide.navigation.NavigationRoot
import ai.tour.guide.navigation.Route
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun AppDrawerLayout(modifier: Modifier = Modifier) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedRoute by remember { mutableStateOf<Route>(Route.Dashboard) }

    ModalNavigationDrawer(
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
                        selectedRoute = route
                        scope.launch { drawerState.close() }
                    }
                )
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            when (selectedRoute) {
                                Route.Dashboard -> stringResource(R.string.navigation_route_dashboard_name)
                                Route.Profile -> stringResource(R.string.navigation_drawer_item_profile)
                                Route.Settings -> stringResource(R.string.navigation_drawer_item_app_settings)
                            }
                        )
                    },
                    navigationIcon = {
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
                )
            },
        ) { innerPadding ->
            NavigationRoot(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun DrawerContent(
    selectedRoute: Route,
    onRouteSelected: (Route) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = stringResource(R.string.navigation_drawer_header),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.titleLarge,
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.navigation_drawer_item_dashboard)) },
            selected = selectedRoute == Route.Dashboard,
            onClick = { onRouteSelected(Route.Dashboard) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = "null"
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
                    contentDescription = "null"
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
                    contentDescription = "null"
                )
            }
        )
        HorizontalDivider(modifier = Modifier.padding(16.dp))
    }
}
