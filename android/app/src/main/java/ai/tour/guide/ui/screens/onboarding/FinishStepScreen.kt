package ai.tour.guide.ui.screens.onboarding

import ai.tour.guide.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun FinishStepScreen(modifier: Modifier = Modifier, onNextClicked: () -> Unit = {}) {
    AnimatedSharedStepScreen(
        onNextClicked,
        headerTitle = stringResource(R.string.onboarding_step5_header_text_header),
        headerBody = stringResource(R.string.onboarding_step5_header_text_body),
        buttonLabel = stringResource(R.string.onboarding_step5_get_started_btn_content)
    )
}