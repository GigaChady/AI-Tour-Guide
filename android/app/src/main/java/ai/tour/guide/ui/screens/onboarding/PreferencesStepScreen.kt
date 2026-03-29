package ai.tour.guide.ui.screens.onboarding

import ai.tour.guide.R
import ai.tour.guide.ui.components.onboarding.OnboardingWelcomeText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun OnboardingPreferencesStepScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        OnboardingWelcomeText(stringResource(R.string.onboarding_step4_header_text_body))
    }
}