package ai.tour.guide.ui.screens

import ai.tour.guide.ui.components.onboarding.OnboardingWelcomeText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OnboardingWelcomeText(headerTitle = "Dashboard", bodyText = "dashboard")
    }
}
