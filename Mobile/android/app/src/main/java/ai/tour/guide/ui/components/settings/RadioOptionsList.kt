package ai.tour.guide.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

@Composable
fun RadioOptionsList(modifier: Modifier = Modifier, options: List<String> = listOf()) {
    var selectedItem by remember { mutableStateOf("") }
    Column(modifier = modifier) {
        options.forEach { option ->
            val isSelected = option == selectedItem
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isSelected,
                        onClick = { selectedItem = option },
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