package ai.tour.guide.ui.screens.main

import ai.tour.guide.R
import ai.tour.guide.ui.components.display.SummaryIconSection
import ai.tour.guide.ui.components.display.TripProgressStepper
import ai.tour.guide.ui.navigation.Route
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

@Composable
@Preview(showBackground = true)
fun TripEndSummaryScreen(modifier: Modifier = Modifier, backStack: NavBackStack<NavKey>? = null) {
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = stringResource(R.string.trip_end_summary_header),
                    style = MaterialTheme.typography.displayMediumEmphasized,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.trip_end_summary_header_subtext),
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.trip_end_summary_quick_summary_section_header),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    color = MaterialTheme.colorScheme.onBackground
                )
                SummaryIconSection()
                Text(
                    text = stringResource(R.string.trip_end_summary_visited_places_section_header),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TripProgressStepper()
            }
            ExtendedFloatingActionButton(
                onClick = {
                    backStack?.clear()
                    backStack?.add(Route.Dashboard)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.trip_end_summary_go_back_cta_button_text),
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                }
            )
        }
    }
}
