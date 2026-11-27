// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.settings

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.model.DailyLearningType
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import com.noamtu.jewishday.data.AppThemeOption
import com.noamtu.jewishday.i18n.AppLocales
import com.noamtu.jewishday.i18n.label
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
import com.noamtu.jewishday.ui.components.ScreenSurface
import com.noamtu.jewishday.ui.components.readableWidth
import com.noamtu.jewishday.widget.WidgetKind

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    // The language the screen is in — chosen here or in the system settings, or the phone's own.
    val activeLanguage = AppLocales.displayTag(LocalConfiguration.current)
    // Asked once, not on every recomposition: it is a binder call, and a launcher that takes pin
    // requests does not stop doing so while the settings are open.
    val canPinWidget = remember(context) { AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported }
    var pendingNotificationTarget by remember { mutableStateOf<NotificationPermissionTarget?>(null) }
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showAddWidgetDialog by rememberSaveable { mutableStateOf(false) }
    var showZmanimTimes by rememberSaveable { mutableStateOf(false) }
    var showDailyLearning by rememberSaveable { mutableStateOf(false) }
    var showAdvancedMethods by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            when (pendingNotificationTarget) {
                NotificationPermissionTarget.HebrewStatusIcon -> viewModel.setHebrewDateStatusIconEnabled(true)
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
                        label = stringResource(R.string.settings_hebrew_status_icon),
                        description = stringResource(R.string.settings_hebrew_status_icon_description, ),
                        checked = uiState.hebrewDateStatusIconEnabled,
                        onCheckedChange = { enabled ->
                            updateNotificationSetting(
                                enabled = enabled,
                                target = NotificationPermissionTarget.HebrewStatusIcon,
                                onAllowed = viewModel::setHebrewDateStatusIconEnabled,
                            )
                        },
                    )
                    SectionSettingsDivider()
                    // Only a launcher that takes pin requests can add a widget from here; on any
                    // other the row would be a dead tap, so it is not offered at all.
                    if (canPinWidget) {
                        SettingsActionRow(
                            label = stringResource(R.string.settings_add_widget),
                            description = stringResource(R.string.settings_add_widget_description, ),
                            onClick = { showAddWidgetDialog = true },
                        )
                        SectionSettingsDivider()
                    }
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_12_hour_format),
                        description = stringResource(R.string.settings_12_hour_format_description, ),
                        // Default is 24-hour (use24HourTime = true). This switch opts in to
                        // 12-hour AM/PM display, so its checked state is the inverse.
                        checked = !uiState.use24HourTime,
                        onCheckedChange = { use12Hour -> viewModel.setUse24HourTime(!use12Hour) },
                    )
                    SectionSettingsDivider()
                    SettingsChoiceRow(
                        label = stringResource(R.string.settings_language),
                        description = stringResource(R.string.settings_language_description, ),
                        value = AppLocales.displayName(activeLanguage),
                        onClick = { showLanguageDialog = true },
                    )
                    SectionSettingsDivider()
                    SettingsChoiceRow(
                        label = stringResource(R.string.settings_theme),
                        value = uiState.themeOption.localizedLabel(),
                        onClick = { showThemeDialog = true },
                    )
                    SectionSettingsDivider()
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_prereleases),
                        description = stringResource(R.string.settings_prereleases_description, ),
                        checked = uiState.includePreReleases,
                        onCheckedChange = viewModel::setIncludePreReleases,
                    )
                    // Pre-releases keep the versionCode of the stable they were built on, so
                    // turning the switch off really does offer a way back. Said here, where the
                    // decision is made, because "test version" alone does not imply it is reversible.
                    uiState.installedPreReleaseName?.let { version ->
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            text = stringResource(R.string.settings_prereleases_installed_note, version,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                SettingsSectionTitle(
                    text = stringResource(R.string.settings_zmanim_options),
                )
            }
            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    ExpandableSettingsHeader(
                        label = stringResource(R.string.settings_advanced_zmanim),
                        description = stringResource(R.string.settings_advanced_zmanim_description),
                        expanded = showAdvancedMethods,
                        resetContentDescription = stringResource(R.string.settings_reset, stringResource(R.string.settings_advanced_zmanim)),
                        onToggle = { showAdvancedMethods = !showAdvancedMethods },
                        onReset = { viewModel.resetZmanimMethods() },
                    )
                    if (showAdvancedMethods) {
                        SettingsDivider()
                        AdvancedZmanimChoices(
                            settings = uiState.zmanimSettings,
                            candleLightingDefault = uiState.candleLightingDefault,
                            offersNoTzeitFallback = uiState.offersNoTzeitFallback,
                            viewModel = viewModel,
                        )
                    }
                }
            }
            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    ExpandableSettingsHeader(
                        label = stringResource(R.string.settings_zmanim_times_section),
                        description = stringResource(R.string.settings_zmanim_times_section_description),
                        expanded = showZmanimTimes,
                        resetContentDescription = stringResource(R.string.settings_reset, stringResource(R.string.settings_zmanim_times_section)),
                        onToggle = { showZmanimTimes = !showZmanimTimes },
                        onReset = { viewModel.resetZmanimTimes() },
                    )
                    if (showZmanimTimes) {
                        // A single divider under the header, then dividerless toggle rows: the
                        // switches themselves delineate the items, so a long on/off list stays
                        // compact and uncluttered.
                        SettingsDivider()
                        ZmanimTimeOption.entries.forEach { option ->
                            SettingsToggleRow(
                                label = stringResource(option.label),
                                checked = option in uiState.enabledZmanimTimes,
                                onCheckedChange = { enabled -> viewModel.setZmanimTimeEnabled(option, enabled) },
                            )
                        }
                    }
                }
            }
            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    ExpandableSettingsHeader(
                        label = stringResource(R.string.settings_daily_learning_section),
                        description = stringResource(R.string.settings_daily_learning_section_description),
                        expanded = showDailyLearning,
                        resetContentDescription = stringResource(R.string.settings_reset, stringResource(R.string.settings_daily_learning_section)),
                        onToggle = { showDailyLearning = !showDailyLearning },
                        onReset = { viewModel.resetDailyLearning() },
                    )
                    if (showDailyLearning) {
                        SettingsDivider()
                        DailyLearningType.entries.forEach { type ->
                            SettingsToggleRow(
                                label = stringResource(type.label),
                                checked = type in uiState.enabledDailyLearning,
                                onCheckedChange = { enabled -> viewModel.setDailyLearningEnabled(type, enabled) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme), modifier = Modifier.semantics { heading() }) },
            text = {
                Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppThemeOption.entries.forEach { themeOption ->
                        ThemeOptionRow(
                            label = themeOption.localizedLabel(),
                            selected = themeOption == uiState.themeOption,
                            onClick = {
                                viewModel.setThemeOption(themeOption)
                                showThemeDialog = false
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }

    if (showAddWidgetDialog) {
        AlertDialog(
            onDismissRequest = { showAddWidgetDialog = false },
            title = { Text(stringResource(R.string.settings_add_widget), modifier = Modifier.semantics { heading() }) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    WidgetKind.entries.groupBy { it.style }.forEach { (style, kinds) ->
                        Text(
                            modifier = Modifier
                                .padding(top = if (style.ordinal == 0) 0.dp else 16.dp, bottom = 4.dp)
                                .semantics { heading() },
                            text = stringResource(style.label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        kinds.forEach { kind ->
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) {
                                        // The launcher takes it from here: it shows its own confirmation
                                        // and places the widget, so the dialog's part is done.
                                        AppWidgetManager.getInstance(context)
                                            .requestPinAppWidget(ComponentName(context, kind.provider), null, null)
                                        showAddWidgetDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                text = stringResource(kind.label),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddWidgetDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language), modifier = Modifier.semantics { heading() }) },
            text = {
                Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppLocales.supported.forEach { language ->
                        ThemeOptionRow(
                            label = AppLocales.displayName(language),
                            selected = language == activeLanguage,
                            onClick = {
                                showLanguageDialog = false
                                if (language != activeLanguage) {
                                    viewModel.setAppLanguage(language)
                                    // Android 13+ applies a per-app language to the running activity
                                    // itself; older versions only read it as the activity is created.
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) activity?.recreate()
                                }
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }

}

private enum class NotificationPermissionTarget {
    HebrewStatusIcon,
}

@Composable
private fun AppThemeOption.localizedLabel(): String = when (this) {
    AppThemeOption.BlueWhite -> stringResource(R.string.theme_blue_white)
    AppThemeOption.OliveGrove -> stringResource(R.string.theme_olive_grove)
    AppThemeOption.JerusalemStone -> stringResource(R.string.theme_jerusalem_stone)
    AppThemeOption.Sand -> stringResource(R.string.theme_sand)
    AppThemeOption.Midnight -> stringResource(R.string.theme_midnight)
    AppThemeOption.Slate -> stringResource(R.string.theme_slate)
    AppThemeOption.AmoledBlack -> stringResource(R.string.theme_amoled_black)
    AppThemeOption.Glass -> stringResource(R.string.theme_glass)
}
