package ai.tour.guide.ui.screens.onboarding

import ai.tour.guide.R
import ai.tour.guide.ui.components.fragments.UserRegistrationFragment
import ai.tour.guide.ui.components.onboarding.OnboardingWelcomeText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun RegisterStepScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onUserRegistered: () -> Unit = {}
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(MaterialTheme.colorScheme.background)
            .then(modifier),
    ) {
        OnboardingWelcomeText(stringResource(R.string.onboarding_step3_header_text_body))
        UserRegistrationFragment(
            onChangesSaved = onUserRegistered,
            ctaButtonText = stringResource(R.string.onboarding_step3_register_button)
        )
    }
}