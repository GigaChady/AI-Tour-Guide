package ai.tour.guide.ui.components.onboarding

import ai.tour.guide.dto.OnboardingPreference
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun OnboardingPreferenceListItem(
    item: OnboardingPreference,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable { onSelected() },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        headlineContent = { Text(item.title) },
        supportingContent = { item.body?.let { Text(it) } },
        trailingContent = { item.trailingContent?.let { Text(it) } }
    )
}