package ai.tour.guide.ui.components.display

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun SummaryIconSection(
    modifier: Modifier = Modifier,
    duration: String = "0 min",
    distance: String = "0.0 km",
    attractions: String = "0"
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        SingleSummarySection(
            iconVector = Icons.Outlined.Schedule,
            sectionName = "Czas",
            content = duration
        )
        SingleSummarySection(
            iconVector = Icons.Outlined.Route,
            sectionName = "Dystans",
            content = distance
        )
        SingleSummarySection(
            iconVector = Icons.Outlined.PinDrop,
            sectionName = "Atrakcje",
            content = attractions
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SingleSummarySection(
    modifier: Modifier = Modifier,
    iconVector: ImageVector = Icons.Default.Schedule,
    sectionName: String = "test",
    content: String = "test"
) {
    Column(
        modifier = Modifier
            .wrapContentSize()
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically)
    ) {
        Column(
            modifier = modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(
                    4.dp,
                    alignment = Alignment.CenterVertically
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Text(text = sectionName, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmallEmphasized)
            }
        }
        Text(
            text = content,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodySmallEmphasized
        )
    }
}