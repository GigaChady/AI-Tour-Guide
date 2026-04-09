package ai.tour.guide.ui.components.fragments

import ai.tour.guide.R
import ai.tour.guide.dto.OnboardingPreference
import ai.tour.guide.dto.OnboardingPreferenceCategory
import ai.tour.guide.dto.OnboardingPreferenceChoiceType
import ai.tour.guide.ui.components.settings.SettingChoiceItem
import ai.tour.guide.ui.components.settings.SettingItemGroupHeader
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun UserPreferenceFragment(
    modifier: Modifier = Modifier,
    trailingSettings: @Composable () -> Unit = {}
) {
    val options = getOptions()
    val groupedOptions = options.groupBy { it.category }
    val selectedOptions = remember {
        mutableStateMapOf<OnboardingPreferenceCategory, OnboardingPreference>()
    }
    Column(
        modifier = modifier
            .padding(16.dp)
    ) {
        LazyColumn {
            groupedOptions.forEach { (category, categoryItems) ->
                item {
                    SettingItemGroupHeader(
                        text = getPreferenceCategoryName(category),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(categoryItems) { item ->
                    SettingChoiceItem(
                        item = item,
                        selected = selectedOptions[category] == item,
                        onSelected = { selectedOptions[category] = item }
                    )
                }
            }
        }
        trailingSettings()
    }
}


@Composable
fun getOptions(): List<OnboardingPreference> {
    return listOf(
        OnboardingPreference(
            stringResource(R.string.onboarding_step4_grammatic_form_option1_main),
            OnboardingPreferenceCategory.GRAMMAR,
            stringResource(R.string.onboarding_step4_grammatic_form_option1_hint),
            null,
            OnboardingPreferenceChoiceType.SINGLE
        ),
        OnboardingPreference(
            stringResource(R.string.onboarding_step4_grammatic_form_option2_main),
            OnboardingPreferenceCategory.GRAMMAR,
            stringResource(R.string.onboarding_step4_grammatic_form_option2_hint),
            null,
            OnboardingPreferenceChoiceType.SINGLE
        ),
        OnboardingPreference(
            stringResource(R.string.onboarding_step4_grammatic_form_option3_main),
            OnboardingPreferenceCategory.GRAMMAR,
            stringResource(R.string.onboarding_step4_grammatic_form_option3_hint),
            null,
            OnboardingPreferenceChoiceType.SINGLE
        ),
        OnboardingPreference(
            stringResource(R.string.onboarding_step4_interests_form_option1_main),
            OnboardingPreferenceCategory.INTERESTS,
            trailingContent = "\uD83C\uDFDB\uFE0F",
            type = OnboardingPreferenceChoiceType.MULTIPLE
        ),
        OnboardingPreference(
            stringResource(R.string.onboarding_step4_interests_form_option2_main),
            OnboardingPreferenceCategory.INTERESTS,
            trailingContent = "\uD83C\uDFD7\uFE0F",
            type = OnboardingPreferenceChoiceType.MULTIPLE
        ),
        OnboardingPreference(
            stringResource(R.string.onboarding_step4_interests_form_option3_main),
            OnboardingPreferenceCategory.INTERESTS,
            trailingContent = "\uD83C\uDFAD",
            type = OnboardingPreferenceChoiceType.MULTIPLE
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