package ai.tour.guide.ui.screens.main

import ai.tour.guide.R
import ai.tour.guide.ui.components.input.SegmentedButton
import ai.tour.guide.ui.components.settings.PickerSetting
import ai.tour.guide.ui.components.settings.RadioOptionsList
import ai.tour.guide.ui.components.settings.SettingGroupHeader
import ai.tour.guide.ui.components.settings.SettingItemWithTitle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun AppSettingsScreen(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            GeneralAppSettingsSection()
            TTSSettingsSection()
            PlaybackSettingsSection()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GeneralAppSettingsSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingGroupHeader(
            title = stringResource(R.string.app_settings_general_section_header),
            subtitle = stringResource(R.string.app_settings_general_section_body)
        )
        SettingItemWithTitle(
            title = stringResource(R.string.app_settings_general_color_scheme_header)
        ) {
            RadioOptionsList(
                options =
                    listOf(
                        stringResource(R.string.app_settings_general_color_scheme_option_auto),
                        stringResource(R.string.app_settings_general_color_scheme_option_dark),
                        stringResource(R.string.app_settings_general_color_scheme_option_light)
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TTSSettingsSection(modifier: Modifier = Modifier) {
    val pitchSliderState = rememberSliderState(value = 0.5f)
    val speedSliderState = rememberSliderState(value = 0.5f)
    
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingGroupHeader(
            title = stringResource(R.string.app_settings_narration_section_header),
            subtitle = stringResource(R.string.app_settings_narration_section_body)
        )
        SettingItemWithTitle(
            title = stringResource(R.string.app_settings_narration_language_header)
        ) {
            PickerSetting(
                title = stringResource(R.string.app_settings_narration_language_sample_selection),
                promptTitle = stringResource(R.string.app_settings_narration_language_picker_header),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null
                    )
                })
        }
        SettingItemWithTitle(
            title = stringResource(R.string.app_settings_narration_pitch_header)
        ) {
            Slider(state = pitchSliderState)
        }
        SettingItemWithTitle(
            title = stringResource(R.string.app_settings_narration_speed_header)
        ) {
            Slider(state = speedSliderState)
        }
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.app_settings_narration_test_settings_button_content),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaybackSettingsSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingGroupHeader(
            title = stringResource(R.string.app_settings_playback_section_header),
            subtitle = stringResource(R.string.app_settings_playback_section_body),
        )
        SettingItemWithTitle(
            title = stringResource(R.string.app_settings_playback_details_count_header)
        ) {
            SegmentedButton(
                options = listOf(
                    stringResource(R.string.app_settings_playback_details_count_option_low),
                    stringResource(R.string.app_settings_playback_details_count_option_mid),
                    stringResource(R.string.app_settings_playback_details_count_option_high)
                )
            )
        }
    }
    SettingItemWithTitle(
        title = stringResource(R.string.app_settings_playback_interrupt_header)
    ) {
        SegmentedButton(
            options = listOf(
                stringResource(R.string.app_settings_playback_interrupt_option_yes),
                stringResource(R.string.app_settings_playback_interrupt_option_no)
            )
        )
    }
}