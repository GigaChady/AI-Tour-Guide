package ai.tour.guide.ui.screens.main.appSettings

import ai.tour.guide.R
import ai.tour.guide.data.appSettings.AppSettingsAppThemeType
import ai.tour.guide.data.appSettings.AppSettingsDetailLevelType
import ai.tour.guide.ui.components.input.SaveButton
import ai.tour.guide.ui.components.input.SegmentedButton
import ai.tour.guide.ui.components.settings.PickerSetting
import ai.tour.guide.ui.components.settings.RadioOptionsList
import ai.tour.guide.ui.components.settings.SettingGroupHeader
import ai.tour.guide.ui.components.settings.SettingItemWithTitle
import ai.tour.guide.ui.navigation.Route
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel

// TODO: Add default loading animation
@Composable
fun AppSettingsScreen(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey>? = null,
    viewModel: AppSettingsViewModel = koinViewModel()
) {
    val viewState by viewModel.viewStateFlow.collectAsStateWithLifecycle()
    val state = viewState.data

    LifecycleStartEffect(Unit) {
        viewModel.onStart()
        onStopOrDispose { }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                GeneralAppSettingsSection(
                    currentTheme = state.appTheme,
                    onThemeChanged = { viewModel.updateTheme(it) }
                )

                AccountSettingsSection(backStack = backStack)

                TTSSettingsSection(
                    pitch = state.pitch,
                    speed = state.speed,
                    onPitchChanged = { viewModel.updatePitch(it) },
                    onSpeedChanged = { viewModel.updateSpeed(it) }
                )

                PlaybackSettingsSection(
                    detailLevel = state.detailLevel,
                    autoPlay = state.autoPlay,
                    onDetailLevelChanged = { viewModel.updateDetailLevel(it) },
                    onAutoPlayChanged = { viewModel.updateAutoPlay(it) }
                )
            }

            // Save button component
            SaveButton(
                onClick = { viewModel.onSaveSettingsClicked() }
            )
        }
    }
}

@Composable
fun GeneralAppSettingsSection(
    currentTheme: AppSettingsAppThemeType,
    onThemeChanged: (AppSettingsAppThemeType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingGroupHeader(
            title = stringResource(R.string.app_settings_general_section_header),
            subtitle = stringResource(R.string.app_settings_general_section_body)
        )
        SettingItemWithTitle(
            title = stringResource(R.string.app_settings_general_color_scheme_header)
        ) {
            val themeOptions = listOf(
                stringResource(R.string.app_settings_general_color_scheme_option_auto),
                stringResource(R.string.app_settings_general_color_scheme_option_dark),
                stringResource(R.string.app_settings_general_color_scheme_option_light)
            )
            // TODO: Optimize
            val selectedIndex = when(currentTheme) {
                AppSettingsAppThemeType.SYSTEM -> 0
                AppSettingsAppThemeType.DARK -> 1
                AppSettingsAppThemeType.LIGHT -> 2
            }

            RadioOptionsList(
                options = themeOptions,
                selectedIndex = selectedIndex,
                onOptionSelected = { index ->
                    val newTheme = when(index) {
                        0 -> AppSettingsAppThemeType.SYSTEM
                        1 -> AppSettingsAppThemeType.DARK
                        else -> AppSettingsAppThemeType.LIGHT
                    }
                    onThemeChanged(newTheme)
                }
            )
        }
    }
}

@Composable
fun TTSSettingsSection(
    pitch: Float,
    speed: Float,
    onPitchChanged: (Float) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
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
            Slider(value = pitch, onValueChange = onPitchChanged, valueRange = 0f..100f)
        }
        SettingItemWithTitle(
            title = stringResource(R.string.app_settings_narration_speed_header)
        ) {
            Slider(value = speed, onValueChange = onSpeedChanged, valueRange = 0f..10f)
        }
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.app_settings_narration_test_settings_button_content),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun PlaybackSettingsSection(
    detailLevel: AppSettingsDetailLevelType,
    autoPlay: Boolean,
    onDetailLevelChanged: (AppSettingsDetailLevelType) -> Unit,
    onAutoPlayChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingGroupHeader(
            title = stringResource(R.string.app_settings_playback_section_header),
            subtitle = stringResource(R.string.app_settings_playback_section_body),
        )
        SettingItemWithTitle(
            title = stringResource(R.string.app_settings_playback_details_count_header)
        ) {
            val detailOptions = listOf(
                stringResource(R.string.app_settings_playback_details_count_option_low),
                stringResource(R.string.app_settings_playback_details_count_option_mid),
                stringResource(R.string.app_settings_playback_details_count_option_high)
            )

            val currentDetailIndex = when (detailLevel) {
                AppSettingsDetailLevelType.LOW -> 0
                AppSettingsDetailLevelType.MEDIUM -> 1
                AppSettingsDetailLevelType.HIGH -> 2
            }

            SegmentedButton(
                options = detailOptions,
                selectedIndex = currentDetailIndex,
                onOptionSelected = { newIndex ->
                    val newLevel = when (newIndex) {
                        0 -> AppSettingsDetailLevelType.LOW
                        1 -> AppSettingsDetailLevelType.MEDIUM
                        else -> AppSettingsDetailLevelType.HIGH
                    }
                    onDetailLevelChanged(newLevel)
                }
            )
        }
        SettingItemWithTitle(
            title = stringResource(R.string.app_settings_playback_interrupt_header)
        ) {
            val autoPlayOptions = listOf(
                stringResource(R.string.app_settings_playback_interrupt_option_yes),
                stringResource(R.string.app_settings_playback_interrupt_option_no)
            )

            val currentAutoPlayIndex = if (autoPlay) 0 else 1

            SegmentedButton(
                options = autoPlayOptions,
                selectedIndex = currentAutoPlayIndex,
                onOptionSelected = { newIndex ->
                    val newAutoPlay = newIndex == 0
                    onAutoPlayChanged(newAutoPlay)
                }
            )
        }
    }
}

@Composable
fun AccountSettingsSection(modifier: Modifier = Modifier, backStack: NavBackStack<NavKey>? = null) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingGroupHeader(
            title = stringResource(R.string.app_settings_account_section_header),
            subtitle = stringResource(R.string.app_settings_account_section_body)
        )
        Button(
            onClick = { backStack?.add(Route.AccountSettings) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                modifier = Modifier.padding(end = 8.dp),
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null
            )
            Text(stringResource(R.string.app_settings_account_section_cta))
        }
    }
}