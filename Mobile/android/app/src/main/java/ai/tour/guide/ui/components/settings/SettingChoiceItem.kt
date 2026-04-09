package ai.tour.guide.ui.components.settings

import ai.tour.guide.dto.OnboardingPreference
import ai.tour.guide.dto.OnboardingPreferenceChoiceType
import androidx.compose.foundation.clickable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingChoiceItem(
    item: OnboardingPreference,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable { onSelected() },
        leadingContent = {
            if (item.type == OnboardingPreferenceChoiceType.SINGLE) {
                RadioButton(selected = selected, onClick = null)
            } else {
                Checkbox(checked = selected, onCheckedChange = null)
            }
        },
        headlineContent = { Text(item.title) },
        supportingContent = { item.body?.let { Text(it) } },
        trailingContent = { item.trailingContent?.let { Text(it) } }
    )
}