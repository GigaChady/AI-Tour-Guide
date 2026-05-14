package ai.tour.guide.ui.sharedFragments.tourSummary

import ai.tour.guide.R
import ai.tour.guide.ui.components.display.SummaryIconSection
import ai.tour.guide.ui.components.display.TripProgressStepper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourSummaryBottomSheet(
    modifier: Modifier = Modifier,
    showBottomSheet: Boolean,
    onDismissRequest: () -> Unit,
    viewModel: TourRouteSummaryViewModel = koinViewModel()
) {
    if (showBottomSheet) {

        LaunchedEffect(Unit) {
            viewModel.onStart()
        }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        val viewState by viewModel.viewStateFlow.collectAsStateWithLifecycle()
        val state = viewState.data
        val scrollState = rememberScrollState()

        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.tour_summary_bottom_sheet_header_text),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall
                )

                SummaryIconSection(
                    duration = viewState.data.durationText,
                    distance = viewState.data.distanceText,
                    attractions = viewState.data.attractionsCountText
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.tour_summary_bottom_sheet_places_section),
                        style = MaterialTheme.typography.titleMedium
                    )
                    TripProgressStepper(
                        places = state.visitedPlaces,
                        activeStopId = state.activeStopId,
                        activeProgress = state.activePlaybackProgress
                    )
                }
            }
        }
    }
}