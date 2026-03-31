package ai.tour.guide.ui.screens.onboarding

import ai.tour.guide.ui.components.onboarding.OnboardingAnimation
import ai.tour.guide.ui.components.onboarding.OnboardingWelcomeText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedSharedStepScreen(
    onNextClicked: () -> Unit = {},
    headerTitle: String,
    headerBody: String,
    buttonLabel: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
            .background(MaterialTheme.colorScheme.background),
    ) {
        OnboardingAnimation()
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
        ) {
            OnboardingWelcomeText(
                bodyText = headerBody,
                headerTitle = headerTitle
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { onNextClicked() },
                    shape = MaterialTheme.shapes.large,
                    icon = { Icon(Icons.AutoMirrored.Default.ArrowForward, null) },
                    text = { Text(buttonLabel) }
                )
            }

        }

    }
}