package ai.tour.guide.ui.screens.main

import ai.tour.guide.R
import ai.tour.guide.ui.screens.onboarding.auth.OnboardingAuthStepViewModel
import ai.tour.guide.ui.sharedFragments.UserRegistrationFragment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import org.koin.compose.viewmodel.koinViewModel

@Preview(showBackground = true)
@Composable
fun AccountSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingAuthStepViewModel = koinViewModel()
) {
    LifecycleStartEffect(Unit) {
        viewModel.onAccountSettingsViewLoaded()
        onStopOrDispose { }
    }
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            Text(
                text = stringResource(R.string.account_settings_header),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                )
            )
            UserRegistrationFragment(
                onChangesSaved = {},
                ctaButtonText = stringResource(R.string.account_settings_cta)
            )
        }
    }
}