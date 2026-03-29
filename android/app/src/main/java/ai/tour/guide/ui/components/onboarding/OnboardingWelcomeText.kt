package ai.tour.guide.ui.components.onboarding

import ai.tour.guide.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingWelcomeText(
    bodyText: String,
    modifier: Modifier = Modifier,
    headerTitle: String? = null
) {
    val header = headerTitle ?: stringResource(R.string.onboarding_header_text_header)
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .padding(
                vertical = 16.dp,
                horizontal = 32.dp,
            )

    ) {
        Text(
            text = header,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = bodyText,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}