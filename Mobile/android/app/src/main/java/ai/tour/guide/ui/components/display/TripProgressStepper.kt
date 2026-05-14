package ai.tour.guide.ui.components.display

import ai.tour.guide.data.route.RouteStopDto
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TripProgressStepper(
    modifier: Modifier = Modifier,
    places: List<RouteStopDto> = emptyList(),
    activeStopId: Int? = null,
    activeProgress: Float = 0f
) {
    val maxSteps = places.size

    Column(modifier = modifier) {
        places.forEachIndexed { index, place ->
            val currentProgress = when {
                activeStopId == null -> 1f
                place.stopId < activeStopId -> 1f
                place.stopId == activeStopId -> activeProgress
                else -> 0f
            }

            TripProgressItem(
                step = index + 1,
                maxSteps = maxSteps,
                progress = currentProgress,
                title = place.title ?: "Punkt trasy",
                description = place.snippet ?: ""
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TripProgressItem(
    modifier: Modifier = Modifier,
    step: Int = 2,
    maxSteps: Int = 3,
    progress: Float = 0f,
    title: String = "Rynek Starego Miasta", //TODO
    description: String = "Serce Starego Miasta" //TODO
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            when {
                maxSteps == 1 -> TripProgressSpinner(step = step, progress = progress)
                step == 1 -> TripProgressLeadingItemStart(step = step, progress = progress)
                step == maxSteps -> TripProgressLeadingItemEnd(step = step, progress = progress)
                else -> TripProgressLeadingItemMiddle(step = step, progress = progress)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TripProgressLeadingItemStart(
    modifier: Modifier = Modifier,
    step: Int = 1,
    progress: Float = 0.5f
) {
    val dividerColor =
        if (progress == 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryFixedDim
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
        ) {
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = 4.dp,
                color = dividerColor
            )
        }
        TripProgressSpinner(step = step, progress = progress)
    }
}

@Preview(showBackground = true)
@Composable
fun TripProgressLeadingItemMiddle(
    modifier: Modifier = Modifier,
    step: Int = 2,
    progress: Float = 0.5f
) {
    val topDividerColor =
        if (progress > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryFixedDim
    val bottomDividerColor =
        if (progress == 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryFixedDim

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxHeight(0.5f)
                .align(Alignment.TopCenter)
        ) {
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = 4.dp,
                color = topDividerColor
            )
        }
        Box(
            modifier = Modifier
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
        ) {
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = 4.dp,
                color = bottomDividerColor
            )
        }
        TripProgressSpinner(step = step, progress = progress)
    }
}

@Preview(showBackground = true)
@Composable
fun TripProgressLeadingItemEnd(
    modifier: Modifier = Modifier,
    step: Int = 3,
    progress: Float = 0.5f
) {
    val dividerColor =
        if (progress > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryFixedDim

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxHeight(0.5f)
                .align(Alignment.TopCenter)
        ) {
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = 4.dp,
                color = dividerColor
            )
        }
        TripProgressSpinner(step = step, progress = progress)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true)
@Composable
fun TripProgressSpinner(modifier: Modifier = Modifier, progress: Float = 1f, step: Int = 1) {
    val sizeModifier = Modifier.size(48.dp)
    Box(modifier = modifier) {
        Box(
            modifier = sizeModifier
                .padding(2.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.toString(),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.headlineSmall
            )
        }
        CircularWavyProgressIndicator(
            progress = { progress },
            modifier = sizeModifier
        )
    }
}