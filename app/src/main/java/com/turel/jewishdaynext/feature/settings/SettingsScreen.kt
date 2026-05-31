package com.turel.jewishdaynext.feature.settings

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turel.jewishdaynext.R
import com.turel.jewishdaynext.ui.components.InfoCard
import com.turel.jewishdaynext.ui.components.ScreenPaddingValues
import com.turel.jewishdaynext.ui.components.ScreenSurface
import com.turel.jewishdaynext.ui.components.readableWidth
import com.turel.jewishdaynext.ui.localizedLocationName
import com.turel.jewishdaynext.ui.localizedString

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingNotificationTarget by remember { mutableStateOf<NotificationPermissionTarget?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            when (pendingNotificationTarget) {
                NotificationPermissionTarget.DailyDate -> viewModel.setDailyDateNotificationEnabled(true)
                NotificationPermissionTarget.HebrewStatusIcon -> viewModel.setHebrewDateStatusIconEnabled(true)
                NotificationPermissionTarget.EnglishStatusIcon -> viewModel.setEnglishDateStatusIconEnabled(true)
                null -> Unit
            }
        }
        pendingNotificationTarget = null
    }

    fun updateNotificationSetting(
        enabled: Boolean,
        target: NotificationPermissionTarget,
        onAllowed: (Boolean) -> Unit,
    ) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingNotificationTarget = target
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onAllowed(enabled)
        }
    }

    ScreenSurface(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .readableWidth()
                .fillMaxSize(),
            contentPadding = ScreenPaddingValues,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SettingsHeader()
            }
            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_daily_notification, R.string.settings_daily_notification_hebrew),
                        description = localizedString(
                            R.string.settings_daily_notification_description,
                            R.string.settings_daily_notification_description_hebrew,
                        ),
                        checked = uiState.dailyDateNotificationEnabled,
                        onCheckedChange = { enabled ->
                            updateNotificationSetting(
                                enabled = enabled,
                                target = NotificationPermissionTarget.DailyDate,
                                onAllowed = viewModel::setDailyDateNotificationEnabled,
                            )
                        },
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_hebrew_status_icon, R.string.settings_hebrew_status_icon_hebrew),
                        description = localizedString(
                            R.string.settings_hebrew_status_icon_description,
                            R.string.settings_hebrew_status_icon_description_hebrew,
                        ),
                        checked = uiState.hebrewDateStatusIconEnabled,
                        onCheckedChange = { enabled ->
                            updateNotificationSetting(
                                enabled = enabled,
                                target = NotificationPermissionTarget.HebrewStatusIcon,
                                onAllowed = viewModel::setHebrewDateStatusIconEnabled,
                            )
                        },
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_english_status_icon, R.string.settings_english_status_icon_hebrew),
                        description = localizedString(
                            R.string.settings_english_status_icon_description,
                            R.string.settings_english_status_icon_description_hebrew,
                        ),
                        checked = uiState.englishDateStatusIconEnabled,
                        onCheckedChange = { enabled ->
                            updateNotificationSetting(
                                enabled = enabled,
                                target = NotificationPermissionTarget.EnglishStatusIcon,
                                onAllowed = viewModel::setEnglishDateStatusIconEnabled,
                            )
                        },
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_hebrew_mode, R.string.settings_hebrew_mode_hebrew),
                        description = localizedString(
                            R.string.settings_hebrew_mode_description,
                            R.string.settings_hebrew_mode_description_hebrew,
                        ),
                        checked = uiState.preferHebrewDates,
                        onCheckedChange = viewModel::setPreferHebrewDates,
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_hebrew_interface, R.string.settings_hebrew_interface_hebrew),
                        description = localizedString(
                            R.string.settings_hebrew_interface_description,
                            R.string.settings_hebrew_interface_description_hebrew,
                        ),
                        checked = uiState.useHebrewInterface,
                        onCheckedChange = viewModel::setUseHebrewInterface,
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_24_hour_time, R.string.settings_24_hour_time_hebrew),
                        description = localizedString(
                            R.string.settings_24_hour_time_description,
                            R.string.settings_24_hour_time_description_hebrew,
                        ),
                        checked = uiState.use24HourTime,
                        onCheckedChange = viewModel::setUse24HourTime,
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_blue_white_theme, R.string.settings_blue_white_theme_hebrew),
                        description = localizedString(
                            R.string.settings_blue_white_theme_description,
                            R.string.settings_blue_white_theme_description_hebrew,
                        ),
                        checked = uiState.blueWhiteTheme,
                        onCheckedChange = viewModel::setBlueWhiteTheme,
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_amoled_theme, R.string.settings_amoled_theme_hebrew),
                        description = localizedString(
                            R.string.settings_amoled_theme_description,
                            R.string.settings_amoled_theme_description_hebrew,
                        ),
                        checked = uiState.amoledBlackTheme,
                        onCheckedChange = viewModel::setAmoledBlackTheme,
                    )
                }
            }
            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = localizedString(R.string.settings_calculation_place, R.string.settings_calculation_place_hebrew),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = localizedString(
                            R.string.settings_calculation_place_description,
                            R.string.settings_calculation_place_description_hebrew,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    uiState.savedPlaces.forEachIndexed { index, place ->
                        SettingsPlaceRow(
                            label = localizedLocationName(place.name),
                            selected = place.id == uiState.selectedPlaceId,
                            onClick = { viewModel.selectPlace(place.id) },
                        )
                        if (index != uiState.savedPlaces.lastIndex) {
                            SettingsDivider()
                        }
                    }
                }
            }
            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = localizedString(R.string.settings_zmanim_options, R.string.settings_zmanim_options_hebrew),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_in_israel, R.string.settings_in_israel_hebrew),
                        description = localizedString(R.string.settings_in_israel_description, R.string.settings_in_israel_description_hebrew),
                        checked = uiState.zmanimSettings.inIsrael,
                        onCheckedChange = viewModel::setInIsrael,
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_use_mga, R.string.settings_use_mga_hebrew),
                        description = localizedString(R.string.settings_use_mga_description, R.string.settings_use_mga_description_hebrew),
                        checked = uiState.zmanimSettings.useMgaForShemaAndTefila,
                        onCheckedChange = viewModel::setUseMgaForShemaAndTefila,
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_sea_level_sunrise, R.string.settings_sea_level_sunrise_hebrew),
                        description = "",
                        checked = uiState.zmanimSettings.useSeaLevelSunrise,
                        onCheckedChange = viewModel::setUseSeaLevelSunrise,
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_sea_level_sunset, R.string.settings_sea_level_sunset_hebrew),
                        description = "",
                        checked = uiState.zmanimSettings.useSeaLevelSunset,
                        onCheckedChange = viewModel::setUseSeaLevelSunset,
                    )
                    SettingsDivider()
                    SettingsChoiceRow(
                        label = localizedString(R.string.settings_alot_offset, R.string.settings_alot_offset_hebrew),
                        value = localizedString(R.string.settings_minutes_value, R.string.settings_minutes_value_hebrew, uiState.zmanimSettings.alotHashacharOffsetMinutes),
                        onClick = {
                            viewModel.setAlotHashacharOffsetMinutes(uiState.zmanimSettings.alotHashacharOffsetMinutes.nextIn(listOf(60, 72, 90, 120)))
                        },
                    )
                    SettingsDivider()
                    SettingsChoiceRow(
                        label = localizedString(R.string.settings_plag_offset, R.string.settings_plag_offset_hebrew),
                        value = if (uiState.zmanimSettings.plagHaminchaOffsetMinutes == 0) {
                            localizedString(R.string.settings_standard_value, R.string.settings_standard_value_hebrew)
                        } else {
                            localizedString(R.string.settings_minutes_value, R.string.settings_minutes_value_hebrew, uiState.zmanimSettings.plagHaminchaOffsetMinutes)
                        },
                        onClick = {
                            viewModel.setPlagHaminchaOffsetMinutes(uiState.zmanimSettings.plagHaminchaOffsetMinutes.nextIn(listOf(0, 60, 72, 90, 96, 120)))
                        },
                    )
                    SettingsDivider()
                    SettingsChoiceRow(
                        label = localizedString(R.string.settings_candle_offset, R.string.settings_candle_offset_hebrew),
                        value = localizedString(R.string.settings_minutes_value, R.string.settings_minutes_value_hebrew, uiState.zmanimSettings.candleLightingOffsetMinutes),
                        onClick = {
                            viewModel.setCandleLightingOffsetMinutes(uiState.zmanimSettings.candleLightingOffsetMinutes.nextIn(listOf(18, 20, 30, 40)))
                        },
                    )
                }
            }
        }
    }
}

private enum class NotificationPermissionTarget {
    DailyDate,
    HebrewStatusIcon,
    EnglishStatusIcon,
}

@Composable
private fun SettingsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier.padding(vertical = 12.dp))
}

@Composable
private fun SettingsHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = localizedString(R.string.settings_summary, R.string.settings_summary_hebrew),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(18.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsChoiceRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(18.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SettingsPlaceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun Int.nextIn(values: List<Int>): Int {
    val index = values.indexOf(this).takeIf { it >= 0 } ?: 0
    return values[(index + 1) % values.size]
}
