package ai.tour.guide.ui.screens.onboarding

import ai.tour.guide.R
import ai.tour.guide.ui.components.onboarding.OnboardingWelcomeText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingRegisterStepScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onUserRegistered: () -> Unit = {}
) {
    val nameFieldState = TextFieldState()
    val emailFieldState = TextFieldState()
    val passwordFieldState = TextFieldState()
    val confirmPasswordFieldState = TextFieldState()

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(MaterialTheme.colorScheme.background)
            .then(modifier),
    ) {
        OnboardingWelcomeText(stringResource(R.string.onboarding_step3_header_text_body))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                state = nameFieldState,
                label = { Text(stringResource(R.string.onboarding_step3_name_input_title)) }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                state = emailFieldState,
                label = { Text(stringResource(R.string.onboarding_step3_email_input_title)) }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                state = passwordFieldState,
                label = { Text(stringResource(R.string.onboarding_step3_password_input_title)) }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                state = confirmPasswordFieldState,
                label = { Text(stringResource(R.string.onboarding_step3_confirm_password_input_title)) }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = { onUserRegistered() },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(Icons.Default.AddCircle, null)
                Text(stringResource(R.string.onboarding_step3_register_button))
            }
        }
    }
}