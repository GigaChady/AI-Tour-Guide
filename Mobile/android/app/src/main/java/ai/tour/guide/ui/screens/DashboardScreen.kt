package ai.tour.guide.ui.screens

import ai.tour.guide.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val headerText = buildAnnotatedString {
        append(stringResource(R.string.dashboard_header_text_content_part1))
        append("\n")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(R.string.dashboard_header_text_content_part2))
        }
    }
    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = headerText,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
