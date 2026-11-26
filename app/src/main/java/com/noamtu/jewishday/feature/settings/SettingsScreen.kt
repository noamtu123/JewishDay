// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.settings

import androidx.annotation.StringRes
import androidx.compose.ui.platform.LocalConfiguration
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noamtu.jewishday.R
import com.noamtu.jewishday.data.AppThemeOption
import com.noamtu.jewishday.model.*
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
import com.noamtu.jewishday.ui.components.ScreenSurface
import com.noamtu.jewishday.i18n.AppLocales
import com.noamtu.jewishday.i18n.LocalTextResolver
import com.noamtu.jewishday.i18n.UiText
import com.noamtu.jewishday.i18n.pluralText
import com.noamtu.jewishday.i18n.resolve
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
                        resetContentDescription = stringResource(R.string.settings_reset),
                        onToggle = { showAdvancedMethods = !showAdvancedMethods },
                        onReset = { viewModel.resetZmanimMethods() },
                    )
                    if (showAdvancedMethods) {
                        SettingsDivider()
                        AdvancedZmanimChoices(
                            settings = uiState.zmanimSettings,
                            candleLightingDefault = uiState.candleLightingDefault,
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
                        resetContentDescription = stringResource(R.string.settings_reset),
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
                        resetContentDescription = stringResource(R.string.settings_reset),
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
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            title = { Text(stringResource(R.string.settings_add_widget)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    WidgetKind.entries.groupBy { it.style }.forEach { (style, kinds) ->
                        Text(
                            modifier = Modifier.padding(top = if (style.ordinal == 0) 0.dp else 16.dp, bottom = 4.dp),
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
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

/**
 * Header for an expandable settings section: tapping the label area expands/collapses it, and a
 * circular reset icon on the side restores that section to its defaults (this replaces the old
 * +/- indicator on every expandable header).
 */
@Composable
private fun ExpandableSettingsHeader(
    label: String,
    description: String,
    resetContentDescription: String,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onToggle),
        ) {
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
        Spacer(Modifier.width(12.dp))
        IconButton(onClick = onReset) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_refresh),
                contentDescription = resetContentDescription,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private enum class NotificationPermissionTarget {
    HebrewStatusIcon,
}

@Composable
private fun SettingsDivider(modifier: Modifier = Modifier) {
    // Hairline divider with no padding of its own — the rows' vertical padding supplies the
    // spacing. Keeps the long expandable zmanim sections compact and quick to scroll.
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp,
    )
}

@Composable
private fun SectionSettingsDivider(modifier: Modifier = Modifier) {
    // The original roomier divider, used only in the top settings card (language, theme, …)
    // where there are few rows and the extra breathing room reads better.
    HorizontalDivider(modifier = modifier.padding(vertical = 12.dp))
}

@Composable
private fun SettingsSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun SettingsHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_summary),
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
    // dense = the tighter look used inside the expandable zmanim sections. The top card leaves
    // this false so it keeps its original roomier styling.
    dense: Boolean = false,
) {
    Row(
        // toggleable rather than clickable + a live Switch: TalkBack then reads the row as a single
        // switch with its label and state, instead of a clickable blob followed by a bare control.
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = if (dense) 8.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description.isNotBlank()) {
                if (!dense) Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = if (dense) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (dense) 2 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(18.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingsChoiceRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = "",
    // dense = the tighter look used by the method rows inside "Detailed Calculation Methods".
    dense: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = if (dense) 8.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description.isNotBlank()) {
                if (!dense) Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = if (dense) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (dense) 2 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(18.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * A row that does something when tapped — asks the launcher to pin the widget — rather than
 * toggling or choosing a value. Modelled on [SettingsChoiceRow], with a chevron where the value
 * would be: with neither a switch nor a value the row would read as plain text, so the chevron is
 * what marks it tappable, in the primary colour the choice rows give their values. Auto-mirrored,
 * it points the way the interface reads.
 */
@Composable
private fun SettingsActionRow(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
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
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_keyboard_arrow_right),
            // Decorative: the label already says what the row does, and the row is one button.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * A bare label + switch row for the long on/off lists ("Zmanim to show", "Limud Yomi to show").
 * No description and no divider between rows — the switches delineate the items — so a big list
 * stays compact and uncluttered. Padding is minimal since the switch's own touch target sets the
 * row height.
 */
@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(18.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
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


@Composable
private fun AdvancedZmanimChoices(
    settings: ZmanimCalculationSettings,
    candleLightingDefault: CandleLightingMethod?,
    viewModel: SettingsViewModel,
) {
    var activePicker by remember { mutableStateOf<ZmanimMethodPicker?>(null) }
    var activeCustomPrompt by remember { mutableStateOf<CustomMethodPrompt?>(null) }
    var showTosefetDialog by rememberSaveable { mutableStateOf(false) }
    var showCandleLightingDialog by rememberSaveable { mutableStateOf(false) }
    var showAteretTorahDialog by rememberSaveable { mutableStateOf(false) }
    val usesAteretTorah = settings.sofZmanShemaMethod == SofZmanShemaMethod.AteretTorah ||
        settings.sofZmanTefillahMethod == SofZmanTefillahMethod.AteretTorah ||
        settings.minchaGedolaMethod == MinchaGedolaMethod.AteretTorah ||
        settings.minchaKetanaMethod == MinchaKetanaMethod.AteretTorah ||
        settings.plagHaminchaMethod == PlagHaminchaMethod.AteretTorah
    // Used only to mark which option is the app default in each picker.
    val defaults = remember { ZmanimCalculationSettings() }

    // Words are looked up here rather than with stringResource, since the pickers are built in click
    // handlers, outside the composition.
    val resolver = LocalTextResolver.current
    fun text(@StringRes id: Int, vararg args: Any): String = resolver.string(id, *args)
    fun UiText.text(): String = resolver.resolve(this)
    fun minutes(count: Int): String = resolver.resolve(pluralText(R.plurals.method_minutes_value, count))

    /**
     * Builds a method picker: the named opinions, and then a single "Custom" row for everything a
     * number can express. The row does not try to say what that number is — choosing it opens a
     * dialog that asks how the zman is measured (degrees, fixed minutes, zmaniyot minutes) and what
     * the value is, which is the only place those two questions make sense together.
     */
    fun <T> picker(
        title: String,
        options: List<T>,
        selected: T,
        default: T,
        label: (T) -> String,
        onSelect: (T) -> Unit,
        // Null for the pickers that have no custom options at all (sunrise, sunset, chatzot).
        customUnit: ((T) -> CustomZmanUnit?)? = null,
        values: CustomZmanValue? = null,
        onSelectCustom: ((T, CustomZmanUnit, Double) -> Unit)? = null,
    ): ZmanimMethodPicker {
        val customs = options.filter { customUnit?.invoke(it) != null }
        val namedOptions = options.filterNot { it in customs }.map { option ->
            val optionLabel = label(option)
            ZmanimMethodOption(
                label = if (option == default) text(R.string.settings_method_option_default, optionLabel) else optionLabel,
                selected = option == selected,
                onSelect = { onSelect(option) },
            )
        }
        if (customs.isEmpty() || values == null || onSelectCustom == null) {
            return ZmanimMethodPicker(title = title, options = namedOptions)
        }
        val customRow = ZmanimMethodOption(
            label = text(R.string.settings_method_custom),
            selected = selected in customs,
            onSelect = {
                activeCustomPrompt = CustomMethodPrompt(
                    title = title,
                    values = values,
                    // The unit it is already on, so reopening the dialog shows what is in effect
                    // rather than starting over at the top of the list.
                    initialIndex = customs.indexOf(selected).coerceAtLeast(0),
                    choices = customs.map { option ->
                        val unit = requireNotNull(customUnit?.invoke(option))
                        CustomMethodChoice(
                            label = label(option),
                            unit = unit,
                            onConfirm = { entered -> onSelectCustom(option, unit, entered) },
                        )
                    },
                )
            },
        )
        return ZmanimMethodPicker(title = title, options = namedOptions + customRow)
    }

    MethodChoiceRow(text(R.string.zman_alot), text(R.string.settings_method_alot_description), settings.alotHashacharMethod.caption(settings).text()) {
        activePicker = picker(
            title = text(R.string.zman_alot),
            options = AlotHashacharMethod.entries,
            selected = settings.alotHashacharMethod,
            default = defaults.alotHashacharMethod,
            label = { it.label.text() },
            onSelect = viewModel::setAlotHashacharMethod,
            customUnit = { it.customUnit },
            values = settings.alotHashacharCustom,
            onSelectCustom = viewModel::setAlotHashacharCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.zman_tallit_tefillin), text(R.string.settings_method_tallit_description), settings.misheyakirMethod.caption(settings).text()) {
        activePicker = picker(
            title = text(R.string.zman_tallit_tefillin),
            options = MisheyakirMethod.entries,
            selected = settings.misheyakirMethod,
            default = defaults.misheyakirMethod,
            label = { it.label.text() },
            onSelect = viewModel::setMisheyakirMethod,
            customUnit = { it.customUnit },
            values = settings.misheyakirCustom,
            onSelectCustom = viewModel::setMisheyakirCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.zman_sunrise), text(R.string.settings_method_sunrise_description), settings.sunriseMethod.label.text()) {
        activePicker = picker(text(R.string.zman_sunrise), SunriseMethod.entries, settings.sunriseMethod, defaults.sunriseMethod, { it.label.text() }, viewModel::setSunriseMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.zman_shema_gra), text(R.string.settings_method_shema_gra_description), settings.sofZmanShemaGraMethod.caption(settings).text()) {
        activePicker = picker(text(R.string.zman_shema_gra), SofZmanShemaMethod.entries.filter { it.family == ZmanOpinionFamily.Gra }, settings.sofZmanShemaGraMethod, defaults.sofZmanShemaGraMethod, { it.label.text() }, viewModel::setSofZmanShemaGraMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.zman_shema_mga), text(R.string.settings_method_shema_mga_description), settings.sofZmanShemaMethod.caption(settings).text()) {
        activePicker = picker(
            title = text(R.string.zman_shema_mga),
            options = SofZmanShemaMethod.entries.filter { it.family == ZmanOpinionFamily.MagenAvraham },
            selected = settings.sofZmanShemaMethod,
            default = defaults.sofZmanShemaMethod,
            label = { it.label.text() },
            onSelect = viewModel::setSofZmanShemaMethod,
            customUnit = { it.customUnit },
            values = settings.sofZmanShemaCustom,
            onSelectCustom = viewModel::setSofZmanShemaCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.zman_tefillah_gra), text(R.string.settings_method_tefillah_gra_description), settings.sofZmanTefillahGraMethod.caption(settings).text()) {
        activePicker = picker(text(R.string.zman_tefillah_gra), SofZmanTefillahMethod.entries.filter { it.family == ZmanOpinionFamily.Gra }, settings.sofZmanTefillahGraMethod, defaults.sofZmanTefillahGraMethod, { it.label.text() }, viewModel::setSofZmanTefillahGraMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.zman_tefillah_mga), text(R.string.settings_method_tefillah_mga_description), settings.sofZmanTefillahMethod.caption(settings).text()) {
        activePicker = picker(
            title = text(R.string.zman_tefillah_mga),
            options = SofZmanTefillahMethod.entries.filter { it.family == ZmanOpinionFamily.MagenAvraham },
            selected = settings.sofZmanTefillahMethod,
            default = defaults.sofZmanTefillahMethod,
            label = { it.label.text() },
            onSelect = viewModel::setSofZmanTefillahMethod,
            customUnit = { it.customUnit },
            values = settings.sofZmanTefillahCustom,
            onSelectCustom = viewModel::setSofZmanTefillahCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.zman_chatzot_hayom), text(R.string.settings_method_chatzot_description), settings.chatzotMethod.label.text()) {
        activePicker = picker(text(R.string.zman_chatzot_hayom), ChatzotMethod.entries, settings.chatzotMethod, defaults.chatzotMethod, { it.label.text() }, viewModel::setChatzotMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.zman_mincha_gedola), text(R.string.settings_method_mincha_gedola_description), settings.minchaGedolaMethod.caption(settings).text()) {
        activePicker = picker(
            title = text(R.string.zman_mincha_gedola),
            options = MinchaGedolaMethod.entries,
            selected = settings.minchaGedolaMethod,
            default = defaults.minchaGedolaMethod,
            label = { it.label.text() },
            onSelect = viewModel::setMinchaGedolaMethod,
            customUnit = { it.customUnit },
            values = settings.minchaGedolaCustom,
            onSelectCustom = viewModel::setMinchaGedolaCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.zman_mincha_ketana), text(R.string.settings_method_mincha_ketana_description), settings.minchaKetanaMethod.caption(settings).text()) {
        activePicker = picker(
            title = text(R.string.zman_mincha_ketana),
            options = MinchaKetanaMethod.entries,
            selected = settings.minchaKetanaMethod,
            default = defaults.minchaKetanaMethod,
            label = { it.label.text() },
            onSelect = viewModel::setMinchaKetanaMethod,
            customUnit = { it.customUnit },
            values = settings.minchaKetanaCustom,
            onSelectCustom = viewModel::setMinchaKetanaCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.zman_plag), text(R.string.settings_method_plag_description), settings.plagHaminchaMethod.caption(settings).text()) {
        activePicker = picker(
            title = text(R.string.zman_plag),
            options = PlagHaminchaMethod.entries,
            selected = settings.plagHaminchaMethod,
            default = defaults.plagHaminchaMethod,
            label = { it.label.text() },
            onSelect = viewModel::setPlagHaminchaMethod,
            customUnit = { it.customUnit },
            values = settings.plagHaminchaCustom,
            onSelectCustom = viewModel::setPlagHaminchaCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.zman_sunset), text(R.string.settings_method_sunset_description), settings.sunsetMethod.label.text()) {
        activePicker = picker(text(R.string.zman_sunset), SunsetMethod.entries, settings.sunsetMethod, defaults.sunsetMethod, { it.label.text() }, viewModel::setSunsetMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.settings_method_tzeit), text(R.string.settings_method_tzeit_description), settings.tzeitHakochavimMethod.caption(settings).text()) {
        activePicker = picker(
            title = text(R.string.settings_method_tzeit),
            options = TzeitHakochavimMethod.entries,
            selected = settings.tzeitHakochavimMethod,
            default = defaults.tzeitHakochavimMethod,
            label = { it.label.text() },
            onSelect = viewModel::setTzeitHakochavimMethod,
            customUnit = { it.customUnit },
            values = settings.tzeitHakochavimCustom,
            onSelectCustom = viewModel::setTzeitHakochavimCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.zman_chatzot_halaila), text(R.string.settings_method_chatzot_halaila_description), settings.chatzotHaLailaMethod.label.text()) {
        activePicker = picker(text(R.string.zman_chatzot_halaila), ChatzotMethod.entries, settings.chatzotHaLailaMethod, defaults.chatzotHaLailaMethod, { it.label.text() }, viewModel::setChatzotHaLailaMethod)
    }
    SettingsDivider()
    // Candle lighting is the one picker whose fixed options are minhagim rather than rungs of a
    // calculation ladder — they are what the first-launch prompt offers — so all four stay, with a
    // typed-in value beside them.
    MethodChoiceRow(text(R.string.settings_method_candle_lighting), text(R.string.settings_method_candle_lighting_description), settings.candleLightingMethod.caption(settings).text()) {
        activePicker = ZmanimMethodPicker(
            title = text(R.string.settings_method_candle_lighting),
            options = CandleLightingMethod.entries.map { option ->
                val default = candleLightingDefault ?: defaults.candleLightingMethod
                val optionLabel = option.label.text()
                ZmanimMethodOption(
                    label = when {
                        // Nothing to ask about the unit here — every option is a minute count — so
                        // the custom row leads straight to a number.
                        option.offsetMinutes == null -> text(R.string.settings_method_custom)
                        option == default -> text(R.string.settings_method_option_default, optionLabel)
                        else -> optionLabel
                    },
                    selected = option == settings.candleLightingMethod,
                    onSelect = {
                        if (option.offsetMinutes == null) {
                            showCandleLightingDialog = true
                        } else {
                            viewModel.setCandleLightingMethod(option)
                        }
                    },
                )
            },
        )
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.zman_motzei_shabbat), text(R.string.settings_method_motzei_description), settings.motzeiShabbatMethod.caption(settings).text()) {
        activePicker = picker(
            title = text(R.string.zman_motzei_shabbat),
            options = MotzeiShabbatMethod.entries,
            selected = settings.motzeiShabbatMethod,
            default = defaults.motzeiShabbatMethod,
            label = { it.label.text() },
            onSelect = viewModel::setMotzeiShabbatMethod,
            customUnit = { it.customUnit },
            values = settings.motzeiShabbatCustom,
            onSelectCustom = viewModel::setMotzeiShabbatCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(
        text(R.string.settings_method_tosefet),
        text(R.string.settings_method_tosefet_description),
        minutes(settings.holyDayTosefetMinutes),
    ) {
        showTosefetDialog = true
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.zman_rabbeinu_tam), text(R.string.settings_method_rabbeinu_tam_description), settings.rabbeinuTamMethod.caption(settings).text()) {
        activePicker = picker(
            title = text(R.string.zman_rabbeinu_tam),
            options = RabbeinuTamMethod.entries,
            selected = settings.rabbeinuTamMethod,
            default = defaults.rabbeinuTamMethod,
            label = { it.label.text() },
            onSelect = viewModel::setRabbeinuTamMethod,
            customUnit = { it.customUnit },
            values = settings.rabbeinuTamCustom,
            onSelectCustom = viewModel::setRabbeinuTamCustomValue,
        )
    }
    // Ateret Torah measures its whole day to a fixed number of minutes after sunset, and that number
    // is the method: Chacham Harari-Raful gave 25 minutes for Israel where KosherJava defaults to 40.
    // Shown only once one of his opinions is in use, since it changes nothing otherwise.
    if (usesAteretTorah) {
        SettingsDivider()
        MethodChoiceRow(
            text(R.string.settings_method_ateret_torah_offset),
            text(R.string.settings_method_ateret_torah_offset_description),
            minutes(settings.ateretTorahSunsetOffsetMinutes),
        ) {
            showAteretTorahDialog = true
        }
    }
    SettingsDivider()
    MethodChoiceRow(text(R.string.settings_method_chametz), text(R.string.settings_method_chametz_description), settings.chametzMethod.caption(settings).text()) {
        activePicker = picker(
            title = text(R.string.settings_method_chametz),
            options = ChametzMethod.entries,
            selected = settings.chametzMethod,
            default = defaults.chametzMethod,
            label = { it.label.text() },
            onSelect = viewModel::setChametzMethod,
            customUnit = { it.customUnit },
            values = settings.chametzCustom,
            onSelectCustom = viewModel::setChametzCustomValue,
        )
    }

    if (showTosefetDialog) {
        MinutesInputDialog(
            title = text(R.string.settings_method_tosefet),
            description = text(R.string.settings_tosefet_dialog),
            initialMinutes = settings.holyDayTosefetMinutes,
            confirmLabel = text(R.string.settings_save),
            dismissLabel = text(R.string.settings_cancel),
            onDismiss = { showTosefetDialog = false },
            onConfirm = { minutes ->
                viewModel.setHolyDayTosefetMinutes(minutes)
                showTosefetDialog = false
            },
        )
    }

    if (showAteretTorahDialog) {
        MinutesInputDialog(
            title = text(R.string.method_ateret_torah),
            description = text(R.string.settings_ateret_torah_dialog),
            initialMinutes = settings.ateretTorahSunsetOffsetMinutes,
            confirmLabel = text(R.string.settings_save),
            dismissLabel = text(R.string.settings_cancel),
            onDismiss = { showAteretTorahDialog = false },
            onConfirm = { minutes ->
                viewModel.setAteretTorahSunsetOffsetMinutes(minutes)
                showAteretTorahDialog = false
            },
        )
    }

    if (showCandleLightingDialog) {
        MinutesInputDialog(
            title = text(R.string.settings_method_candle_lighting),
            description = text(R.string.settings_candle_lighting_dialog),
            initialMinutes = settings.candleLightingCustomMinutes,
            confirmLabel = text(R.string.settings_save),
            dismissLabel = text(R.string.settings_cancel),
            onDismiss = { showCandleLightingDialog = false },
            onConfirm = { minutes ->
                viewModel.setCandleLightingCustomMinutes(minutes)
                showCandleLightingDialog = false
            },
        )
    }

    activeCustomPrompt?.let { prompt ->
        CustomMethodDialog(
            prompt = prompt,
            description = text(R.string.settings_custom_method_dialog),
            confirmLabel = text(R.string.settings_save),
            dismissLabel = text(R.string.settings_cancel),
            onDismiss = { activeCustomPrompt = null },
            onConfirm = { choice, value ->
                choice.onConfirm(value)
                activeCustomPrompt = null
            },
        )
    }

    activePicker?.let { pickerConfig ->
        AlertDialog(
            onDismissRequest = { activePicker = null },
            title = { Text(pickerConfig.title) },
            text = {
                LazyColumn(
                    // Sized to its options (most zmanim have two or three), scrolling only past the cap.
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(pickerConfig.options.size) { index ->
                        val option = pickerConfig.options[index]
                        ThemeOptionRow(
                            label = option.label,
                            selected = option.selected,
                            onClick = {
                                // A custom option opens its own entry field rather than selecting
                                // outright; either way the picker has done its job.
                                option.onSelect()
                                activePicker = null
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { activePicker = null }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
}

private data class ZmanimMethodPicker(
    val title: String,
    val options: List<ZmanimMethodOption>,
)

private data class ZmanimMethodOption(
    val label: String,
    val selected: Boolean,
    val onSelect: () -> Unit,
)

/** One way a zman can be measured by a number, and how to store a value measured that way. */
private data class CustomMethodChoice(
    val label: String,
    val unit: CustomZmanUnit,
    val onConfirm: (Double) -> Unit,
)

/** An open "Custom" dialog: the ways this zman can be measured, and the numbers already held. */
private data class CustomMethodPrompt(
    val title: String,
    val choices: List<CustomMethodChoice>,
    val initialIndex: Int,
    val values: CustomZmanValue,
)

/**
 * The custom dialog: pick how the zman is measured, then enter the value. Both questions belong
 * together — a number means nothing without its unit — and this is the only place either is asked.
 *
 * Switching the unit re-fills the field from that unit's own stored number, so 16.1° and 72 minutes
 * both survive being looked at. Degrees take a decimal point (19.848° is a real opinion); the minute
 * units are whole minutes, the only precision they are ever stated in.
 */
@Composable
private fun CustomMethodDialog(
    prompt: CustomMethodPrompt,
    description: String,
    confirmLabel: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (CustomMethodChoice, Double) -> Unit,
) {
    var selectedIndex by rememberSaveable(prompt.title) { mutableStateOf(prompt.initialIndex) }
    val index = selectedIndex.coerceIn(prompt.choices.indices)
    val choice = prompt.choices[index]
    val decimal = choice.unit == CustomZmanUnit.Degrees
    // Keyed on the unit as well, so choosing a different one reloads its value rather than carrying
    // a degree figure over into a minute field.
    var value by rememberSaveable(prompt.title, index) {
        mutableStateOf(formatValue(prompt.values.value(choice.unit), decimal))
    }
    val entered = value.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(prompt.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(description, style = MaterialTheme.typography.bodyMedium)
                // A zman measured only one way has nothing to choose, so it goes straight to the field.
                if (prompt.choices.size > 1) {
                    prompt.choices.forEachIndexed { choiceIndex, option ->
                        ThemeOptionRow(
                            label = option.label,
                            selected = choiceIndex == index,
                            onClick = { selectedIndex = choiceIndex },
                        )
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { typed ->
                        val allowed = typed.all { it.isDigit() || (decimal && it == '.') }
                        if (allowed && typed.count { it == '.' } <= 1 && typed.length <= 7) value = typed
                    },
                    singleLine = true,
                    label = { Text(choice.label) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { entered?.let { onConfirm(choice, it) } },
                enabled = entered != null,
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}

/** The stored value as the field should first show it: no trailing ".0", and no point at all in a
 * field that only takes whole minutes. */
private fun formatValue(value: Double, decimal: Boolean): String =
    if (!decimal || value % 1.0 == 0.0) value.toInt().toString() else value.toString()

/** A small numeric entry dialog for a minutes-valued setting. Digits only, capped at two of them. */
@Composable
private fun MinutesInputDialog(
    title: String,
    description: String,
    initialMinutes: Int,
    confirmLabel: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initialMinutes.toString()) }
    val minutes = value.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(description, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = value,
                    onValueChange = { entered ->
                        // Keep it to a plain 0-99 number; anything else simply isn't accepted.
                        if (entered.length <= 2 && entered.all(Char::isDigit)) value = entered
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { minutes?.let(onConfirm) }, enabled = minutes != null) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}

@Composable
private fun MethodChoiceRow(
    title: String,
    description: String,
    value: String,
    onClick: () -> Unit,
) {
    SettingsChoiceRow(
        label = title,
        description = description,
        value = value,
        onClick = onClick,
        dense = true,
    )
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}