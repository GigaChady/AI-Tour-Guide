package ai.tour.guide.ui.sharedFragments

import ai.tour.guide.R
import ai.tour.guide.ui.components.display.SummaryIconSection
import ai.tour.guide.ui.components.display.TripProgressStepper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TourSummaryBottomSheet(
    modifier: Modifier = Modifier,
    showBottomSheet: Boolean = true,
    onDismissRequest: () -> Unit = {}
) {
    if (showBottomSheet) {
        TourSummaryBottomSheetContent(modifier, onDismissRequest = onDismissRequest)
    }
}

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourSummaryBottomSheetContent(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ModalBottomSheet(
        modifier = modifier
            .fillMaxHeight(),
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.tour_summary_bottom_sheet_header_text),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displaySmallEmphasized,
            )
            SummaryIconSection()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.tour_summary_bottom_sheet_places_section),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                )
                TripProgressStepper()
            }
        }
    }
}
