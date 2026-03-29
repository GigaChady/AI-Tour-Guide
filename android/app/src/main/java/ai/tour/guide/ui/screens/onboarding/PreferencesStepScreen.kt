package ai.tour.guide.ui.screens.onboarding

import ai.tour.guide.R
import ai.tour.guide.dto.OnboardingPreference
import ai.tour.guide.dto.OnboardingPreferenceCategory
import ai.tour.guide.ui.components.onboarding.OnboardingPreferenceListItem
import ai.tour.guide.ui.components.onboarding.OnboardingWelcomeText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun PreferencesStepScreen(
    modifier: Modifier = Modifier,
    onNextClicked: () -> Unit = {}
) {
    val options = getOptions()
    val groupedOptions = options.groupBy { it.category }
    val selectedOptions = remember {
        mutableStateMapOf<OnboardingPreferenceCategory, OnboardingPreference>()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        OnboardingWelcomeText(stringResource(R.string.onboarding_step4_header_text_body))
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f)
        ) {
            groupedOptions.forEach { (category, categoryItems) ->
                item {
                    Text(
                        text = getPreferenceCategoryName(category),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = 8.dp
                        )
                    )
                }
                items(categoryItems) { item ->
                    OnboardingPreferenceListItem(
                        item = item,
                        selected = selectedOptions[category] == item,
                        onSelected = { selectedOptions[category] = item }
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = stringResource(R.string.onboarding_step4_page_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            ExtendedFloatingActionButton(
                onClick = { onNextClicked() },
                shape = MaterialTheme.shapes.small,
                icon = { Icon(Icons.AutoMirrored.Default.ArrowForward, null) },
                text = { Text(stringResource(R.string.onboarding_step4_next_button)) }
            )
        }
    }
}

@Composable
fun getOptions(): List<OnboardingPreference> {
    return listOf(
        OnboardingPreference(
            stringResource(R.string.onboarding_step4_grammatic_form_option1_main),
            OnboardingPreferenceCategory.GRAMMAR,
            stringResource(R.string.onboarding_step4_grammatic_form_option1_hint),
        ),
        OnboardingPreference(
            stringResource(R.string.onboarding_step4_grammatic_form_option2_main),
            OnboardingPreferenceCategory.GRAMMAR,
            stringResource(R.string.onboarding_step4_grammatic_form_option2_hint),
        ),
        OnboardingPreference(
            stringResource(R.string.onboarding_step4_grammatic_form_option3_main),
            OnboardingPreferenceCategory.GRAMMAR,
            stringResource(R.string.onboarding_step4_grammatic_form_option3_hint),
        ),
        OnboardingPreference(
            stringResource(R.string.onboarding_step4_interests_form_option1_main),
            OnboardingPreferenceCategory.INTERESTS,
            trailingContent = "\uD83C\uDFDB\uFE0F"
        ),
        OnboardingPreference(
            stringResource(R.string.onboarding_step4_interests_form_option2_main),
            OnboardingPreferenceCategory.INTERESTS,
            trailingContent = "\uD83C\uDFD7\uFE0F"
        ),
        OnboardingPreference(
            stringResource(R.string.onboarding_step4_interests_form_option3_main),
            OnboardingPreferenceCategory.INTERESTS,
            trailingContent = "\uD83C\uDFAD"
        ),
    )
}

@Composable
fun getPreferenceCategoryName(category: OnboardingPreferenceCategory): String {
    return when (category) {
        OnboardingPreferenceCategory.GRAMMAR -> stringResource(R.string.onboarding_step4_grammatic_form_section_header)
        OnboardingPreferenceCategory.INTERESTS -> stringResource(R.string.onboarding_step4_interests_form_section_header)
    }
}