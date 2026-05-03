package ai.tour.guide.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

@Composable
fun RadioOptionsList(
    modifier: Modifier = Modifier,
    options: List<String> = listOf(),
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    Column(modifier = modifier) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isSelected,
                        onClick = { onOptionSelected(index) },
                        role = Role.RadioButton
                    ),
                leadingContent = {
                    RadioButton(
                        selected = isSelected,
                        onClick = null
                    )
                },
                headlineContent = { Text(text = option) },
            )
        }
    }
}