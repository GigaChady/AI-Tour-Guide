package ai.tour.guide.ui.screens.main

import ai.tour.guide.R
import ai.tour.guide.ui.navigation.Route
import ai.tour.guide.ui.sharedFragments.preferences.UserPreferenceFragment
import ai.tour.guide.ui.sharedFragments.preferences.UserPreferenceFragmentViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfilePreferencesScreen(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
    viewModel: UserPreferenceFragmentViewModel = koinViewModel()
) {
    LifecycleStartEffect(Unit) {
        viewModel.onStart()
        onStopOrDispose { }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            UserPreferenceFragment(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.profile_preferences_account_settings_section_header),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 8.dp
                )
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = { backStack.add(Route.AccountSettings) },
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Icon(
                    modifier = Modifier.padding(end = 8.dp),
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null
                )
                Text(stringResource(R.string.profile_preferences_account_settings_cta))
            }
        }
    }
}
