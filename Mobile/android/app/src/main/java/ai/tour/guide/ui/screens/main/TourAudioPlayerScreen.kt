package ai.tour.guide.ui.screens.main

import ai.tour.guide.R
import ai.tour.guide.ui.components.audio.AudioPlayerWidget
import ai.tour.guide.ui.components.fragments.TourSummaryBottomSheet
import ai.tour.guide.ui.navigation.Route
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

@Preview(showBackground = true)
@Composable
fun TourAudioPlayerScreen(modifier: Modifier = Modifier, backStack: NavBackStack<NavKey>? = null) {
    var showBottomSheet by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        TourSummaryBottomSheet(
            showBottomSheet = showBottomSheet,
            onDismissRequest = { showBottomSheet = false })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.tour_audio_player_location_header),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = stringResource(R.string.tour_audio_player_example_location),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Image(
                modifier = Modifier
                    .fillMaxHeight(0.3f)
                    .fillMaxWidth()
                    .padding(0.dp),
                painter = painterResource(R.drawable.dashboard_example_img_2),
                contentDescription = null
            )
            Text(
                text = stringResource(R.string.tour_audio_player_narration_header_title),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelLarge
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = buildNarrationSpannableText(),
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            AudioPlayerWidget(onEndClicked = {
                backStack?.clear()
                backStack?.add(Route.TripEndSummary)
            }, onSpeakerClicked = {
                showBottomSheet = true
            })
        }
    }
}

@Composable
fun buildNarrationSpannableText(): AnnotatedString {
    val beforeText =
        stringResource(R.string.tour_audio_player_narration_header_example_content_before)
    val currentText =
        stringResource(R.string.tour_audio_player_narration_header_example_content_current)
    val afterText =
        stringResource(R.string.tour_audio_player_narration_header_example_content_after)

    return buildAnnotatedString {
        append(beforeText)
        append(" ")
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
            append(currentText)
        }
        append(" ")
        append(afterText)
    }
}