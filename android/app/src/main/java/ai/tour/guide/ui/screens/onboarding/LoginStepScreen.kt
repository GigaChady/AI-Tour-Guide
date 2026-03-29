package ai.tour.guide.ui.screens.onboarding

import ai.tour.guide.R
import ai.tour.guide.ui.components.onboarding.OnboardingWelcomeText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun LoginStepScreen(
    onRegisterSpanClicked: () -> Unit = {},
    onLoginFinished: () -> Unit = {}
) {
    val emailFieldState = TextFieldState()
    val passwordFieldState = TextFieldState()
    val registerLinkText = buildAnnotatedString {
        append(stringResource(R.string.onboarding_step2_no_account_question))
        append(" ")
        withLink(
            LinkAnnotation.Clickable(
                tag = "register",
                styles = TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary)),
                linkInteractionListener = { onRegisterSpanClicked() }
            )
        ) {
            append(stringResource(R.string.onboarding_step2_register_span))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
            .background(MaterialTheme.colorScheme.background),
    ) {
        OnboardingWelcomeText(stringResource(R.string.onboarding_step2_header_text_body))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                state = emailFieldState,
                label = { Text(stringResource(R.string.onboarding_step2_email_input_title)) }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                state = passwordFieldState,
                label = { Text(stringResource(R.string.onboarding_step2_password_input_title)) }
            )
            Text(
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                text = registerLinkText
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onLoginFinished() },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(Icons.Default.AccountCircle, null)
                Text(stringResource(R.string.onboarding_step2_login_button))
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth(0.6f))
            Button(
                onClick = { onLoginFinished() },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(stringResource(R.string.onboarding_step2_login_with_google_button))
            }
        }
    }
}
