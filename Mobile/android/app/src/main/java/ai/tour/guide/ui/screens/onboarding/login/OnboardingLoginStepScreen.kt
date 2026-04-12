package ai.tour.guide.ui.screens.onboarding.login

import ai.tour.guide.R
import ai.tour.guide.ui.components.onboarding.LoadingOverlay
import ai.tour.guide.ui.components.onboarding.OnboardingWelcomeText
import ai.tour.guide.ui.navigation.Route
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel

@Preview(showBackground = true)
@Composable
fun OnboardingLoginStepScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingLoginStepViewModel = koinViewModel(),
    backStack: NavBackStack<NavKey>? = null
) {
    val context = LocalContext.current
    val viewModelState by viewModel.stateFlow.collectAsState()

    val registerLinkText = buildAnnotatedString {
        append(stringResource(R.string.onboarding_step2_no_account_question))
        append(" ")
        withLink(
            LinkAnnotation.Clickable(
                tag = "register",
                styles = TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary)),
                linkInteractionListener = { backStack?.add(Route.OnboardingRegisterStepScreen) }
            )
        ) {
            append(stringResource(R.string.onboarding_step2_register_span))
        }
    }

    LaunchedEffect(viewModelState.errorMessage) {
        viewModelState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
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
                        value = viewModelState.email,
                        onValueChange = { viewModel.onEmailChanged(it) },
                        label = { Text(stringResource(R.string.onboarding_step2_email_input_title)) }
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = viewModelState.password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        visualTransformation = PasswordVisualTransformation(),
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
                        onClick = { viewModel.onLoginClicked() },
                        enabled = !viewModelState.isLoading,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Icon(Icons.Default.AccountCircle, null)
                        Text(stringResource(R.string.onboarding_step2_login_button))
                    }
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(0.6f))
                    Button(
                        onClick = { viewModel.onSignInWithGoogleClicked(context) },
                        enabled = !viewModelState.isLoading,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text(stringResource(R.string.onboarding_step2_login_with_google_button))
                    }
                }
            }

            LoadingOverlay(isVisible = viewModelState.isLoading)
        }
    }
}
