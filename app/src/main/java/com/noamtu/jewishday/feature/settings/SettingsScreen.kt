// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.settings

import android.Manifest
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
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
import com.noamtu.jewishday.data.AppLanguage
import com.noamtu.jewishday.data.AppThemeOption
import com.noamtu.jewishday.model.*
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
import com.noamtu.jewishday.ui.components.ScreenSurface
import com.noamtu.jewishday.ui.LocalUseHebrewInterface
import com.noamtu.jewishday.ui.components.readableWidth
import com.noamtu.jewishday.ui.localizedString

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingNotificationTarget by remember { mutableStateOf<NotificationPermissionTarget?>(null) }
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
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
                    SectionSettingsDivider()
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_12_hour_format, R.string.settings_12_hour_format_hebrew),
                        description = localizedString(
                            R.string.settings_12_hour_format_description,
                            R.string.settings_12_hour_format_description_hebrew,
                        ),
                        // Default is 24-hour (use24HourTime = true). This switch opts in to
                        // 12-hour AM/PM display, so its checked state is the inverse.
                        checked = !uiState.use24HourTime,
                        onCheckedChange = { use12Hour -> viewModel.setUse24HourTime(!use12Hour) },
                    )
                    SectionSettingsDivider()
                    SettingsChoiceRow(
                        label = localizedString(R.string.settings_language, R.string.settings_language_hebrew),
                        description = localizedString(
                            R.string.settings_language_description,
                            R.string.settings_language_description_hebrew,
                        ),
                        value = uiState.language.displayName,
                        onClick = { showLanguageDialog = true },
                    )
                    SectionSettingsDivider()
                    SettingsChoiceRow(
                        label = localizedString(R.string.settings_theme, R.string.settings_theme_hebrew),
                        value = uiState.themeOption.localizedLabel(),
                        onClick = { showThemeDialog = true },
                    )
                    SectionSettingsDivider()
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_prereleases, R.string.settings_prereleases_hebrew),
                        description = localizedString(
                            R.string.settings_prereleases_description,
                            R.string.settings_prereleases_description_hebrew,
                        ),
                        checked = uiState.includePreReleases,
                        onCheckedChange = viewModel::setIncludePreReleases,
                    )
                    // Pre-releases keep the versionCode of the stable they were built on, so
                    // turning the switch off really does offer a way back. Said here, where the
                    // decision is made, because "test version" alone does not imply it is reversible.
                    uiState.installedPreReleaseName?.let { version ->
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            text = localizedString(
                                R.string.settings_prereleases_installed_note,
                                R.string.settings_prereleases_installed_note_hebrew,
                                version,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                SettingsSectionTitle(
                    text = localizedString(R.string.settings_zmanim_options, R.string.settings_zmanim_options_hebrew),
                )
            }
            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    ExpandableSettingsHeader(
                        label = localizedString(R.string.settings_advanced_zmanim, R.string.settings_advanced_zmanim_hebrew),
                        description = localizedString(R.string.settings_advanced_zmanim_description, R.string.settings_advanced_zmanim_description_hebrew),
                        resetContentDescription = localizedString(R.string.settings_reset, R.string.settings_reset_hebrew),
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
                val useHebrew = LocalUseHebrewInterface.current
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    ExpandableSettingsHeader(
                        label = localizedString(R.string.settings_zmanim_times_section, R.string.settings_zmanim_times_section_hebrew),
                        description = localizedString(R.string.settings_zmanim_times_section_description, R.string.settings_zmanim_times_section_description_hebrew),
                        resetContentDescription = localizedString(R.string.settings_reset, R.string.settings_reset_hebrew),
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
                                label = if (useHebrew) option.labelHebrew else option.labelEnglish,
                                checked = option in uiState.enabledZmanimTimes,
                                onCheckedChange = { enabled -> viewModel.setZmanimTimeEnabled(option, enabled) },
                            )
                        }
                    }
                }
            }
            item {
                val useHebrew = LocalUseHebrewInterface.current
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    ExpandableSettingsHeader(
                        label = localizedString(R.string.settings_daily_learning_section, R.string.settings_daily_learning_section_hebrew),
                        description = localizedString(R.string.settings_daily_learning_section_description, R.string.settings_daily_learning_section_description_hebrew),
                        resetContentDescription = localizedString(R.string.settings_reset, R.string.settings_reset_hebrew),
                        onToggle = { showDailyLearning = !showDailyLearning },
                        onReset = { viewModel.resetDailyLearning() },
                    )
                    if (showDailyLearning) {
                        SettingsDivider()
                        DailyLearningType.entries.forEach { type ->
                            SettingsToggleRow(
                                label = if (useHebrew) type.labelHebrew else type.labelEnglish,
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
            title = { Text(localizedString(R.string.settings_theme, R.string.settings_theme_hebrew)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.availableThemes.forEach { themeOption ->
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
                    Text(localizedString(R.string.settings_cancel, R.string.settings_cancel_hebrew))
                }
            },
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(localizedString(R.string.settings_language, R.string.settings_language_hebrew)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppLanguage.entries.forEach { language ->
                        ThemeOptionRow(
                            label = language.displayName,
                            selected = language == uiState.language,
                            onClick = {
                                viewModel.setAppLanguage(language)
                                showLanguageDialog = false
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(localizedString(R.string.settings_cancel, R.string.settings_cancel_hebrew))
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
                imageVector = Icons.Outlined.Refresh,
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
    AppThemeOption.BlueWhite -> localizedString(R.string.theme_blue_white, R.string.theme_blue_white_hebrew)
    AppThemeOption.OliveGrove -> localizedString(R.string.theme_olive_grove, R.string.theme_olive_grove_hebrew)
    AppThemeOption.JerusalemStone -> localizedString(R.string.theme_jerusalem_stone, R.string.theme_jerusalem_stone_hebrew)
    AppThemeOption.Sand -> localizedString(R.string.theme_sand, R.string.theme_sand_hebrew)
    AppThemeOption.Midnight -> localizedString(R.string.theme_midnight, R.string.theme_midnight_hebrew)
    AppThemeOption.Slate -> localizedString(R.string.theme_slate, R.string.theme_slate_hebrew)
    AppThemeOption.AmoledBlack -> localizedString(R.string.theme_amoled_black, R.string.theme_amoled_black_hebrew)
    AppThemeOption.Glass -> localizedString(R.string.theme_glass, R.string.theme_glass_hebrew)
}

private fun AlotHashacharMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun MisheyakirMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun SunriseMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun SofZmanShemaMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun SofZmanTefillahMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun ChatzotMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun MinchaGedolaMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun MinchaKetanaMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun PlagHaminchaMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun SunsetMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun TzeitHakochavimMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun CandleLightingMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun MotzeiShabbatMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun RabbeinuTamMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun ChametzMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label

@Composable
private fun AdvancedZmanimChoices(
    settings: ZmanimCalculationSettings,
    candleLightingDefault: CandleLightingMethod?,
    viewModel: SettingsViewModel,
) {
    val useHebrew = LocalUseHebrewInterface.current
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

    fun text(english: String, hebrew: String): String = if (useHebrew) hebrew else english

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
                label = if (option == default) text("$optionLabel (default)", "$optionLabel (ברירת מחדל)") else optionLabel,
                selected = option == selected,
                onSelect = { onSelect(option) },
            )
        }
        if (customs.isEmpty() || values == null || onSelectCustom == null) {
            return ZmanimMethodPicker(title = title, options = namedOptions)
        }
        val customRow = ZmanimMethodOption(
            label = text("Custom", "מותאם אישית"),
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

    MethodChoiceRow(text("Alot Hashachar", "עלות השחר"), text("Dawn start used for Magen Avraham and fast days.", "תחילת היום למג״א ולתעניות."), settings.alotHashacharMethod.caption(settings, useHebrew)) {
        activePicker = picker(
            title = text("Alot Hashachar", "עלות השחר"),
            options = AlotHashacharMethod.entries,
            selected = settings.alotHashacharMethod,
            default = defaults.alotHashacharMethod,
            label = { it.localizedLabel(useHebrew) },
            onSelect = viewModel::setAlotHashacharMethod,
            customUnit = { it.customUnit },
            values = settings.alotHashacharCustom,
            onSelectCustom = viewModel::setAlotHashacharCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text("Tallit & Tefillin", "זמן טלית ותפילין"), text("Earliest tallit and tefillin time (misheyakir).", "הזמן המוקדם לטלית ותפילין (משיכיר)."), settings.misheyakirMethod.caption(settings, useHebrew)) {
        activePicker = picker(
            title = text("Tallit & Tefillin", "זמן טלית ותפילין"),
            options = MisheyakirMethod.entries,
            selected = settings.misheyakirMethod,
            default = defaults.misheyakirMethod,
            label = { it.localizedLabel(useHebrew) },
            onSelect = viewModel::setMisheyakirMethod,
            customUnit = { it.customUnit },
            values = settings.misheyakirCustom,
            onSelectCustom = viewModel::setMisheyakirCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text("Sunrise", "הנץ החמה"), text("Sea level is the common zmanim base; observed uses elevation.", "מישור הוא בסיס נפוץ לזמנים; נראית משתמשת בגובה."), settings.sunriseMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Sunrise", "הנץ החמה"), SunriseMethod.entries, settings.sunriseMethod, defaults.sunriseMethod, { it.localizedLabel(useHebrew) }, viewModel::setSunriseMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Sof Zman Shema (GRA)", "סוף זמן קריאת שמע (גר״א)"), text("Method for the GRA Shema row.", "השיטה לשורת ק״ש של הגר״א."), settings.sofZmanShemaGraMethod.caption(settings, useHebrew)) {
        activePicker = picker(text("Sof Zman Shema (GRA)", "סוף זמן קריאת שמע (גר״א)"), SofZmanShemaMethod.entries.filter { it.family == ZmanOpinionFamily.Gra }, settings.sofZmanShemaGraMethod, defaults.sofZmanShemaGraMethod, { it.localizedLabel(useHebrew) }, viewModel::setSofZmanShemaGraMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Sof Zman Shema (Magen Avraham)", "סוף זמן קריאת שמע (מג״א)"), text("Method for the Magen Avraham Shema row.", "השיטה לשורת ק״ש של מג״א."), settings.sofZmanShemaMethod.caption(settings, useHebrew)) {
        activePicker = picker(
            title = text("Sof Zman Shema (Magen Avraham)", "סוף זמן קריאת שמע (מג״א)"),
            options = SofZmanShemaMethod.entries.filter { it.family == ZmanOpinionFamily.MagenAvraham },
            selected = settings.sofZmanShemaMethod,
            default = defaults.sofZmanShemaMethod,
            label = { it.localizedLabel(useHebrew) },
            onSelect = viewModel::setSofZmanShemaMethod,
            customUnit = { it.customUnit },
            values = settings.sofZmanShemaCustom,
            onSelectCustom = viewModel::setSofZmanShemaCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text("Sof Zman Tefillah (GRA)", "סוף זמן תפילה (גר״א)"), text("Method for the GRA Tefillah row.", "השיטה לשורת תפילה של הגר״א."), settings.sofZmanTefillahGraMethod.caption(settings, useHebrew)) {
        activePicker = picker(text("Sof Zman Tefillah (GRA)", "סוף זמן תפילה (גר״א)"), SofZmanTefillahMethod.entries.filter { it.family == ZmanOpinionFamily.Gra }, settings.sofZmanTefillahGraMethod, defaults.sofZmanTefillahGraMethod, { it.localizedLabel(useHebrew) }, viewModel::setSofZmanTefillahGraMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Sof Zman Tefillah (Magen Avraham)", "סוף זמן תפילה (מג״א)"), text("Method for the Magen Avraham Tefillah row.", "השיטה לשורת תפילה של מג״א."), settings.sofZmanTefillahMethod.caption(settings, useHebrew)) {
        activePicker = picker(
            title = text("Sof Zman Tefillah (Magen Avraham)", "סוף זמן תפילה (מג״א)"),
            options = SofZmanTefillahMethod.entries.filter { it.family == ZmanOpinionFamily.MagenAvraham },
            selected = settings.sofZmanTefillahMethod,
            default = defaults.sofZmanTefillahMethod,
            label = { it.localizedLabel(useHebrew) },
            onSelect = viewModel::setSofZmanTefillahMethod,
            customUnit = { it.customUnit },
            values = settings.sofZmanTefillahCustom,
            onSelectCustom = viewModel::setSofZmanTefillahCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text("Chatzot HaYom", "חצות היום"), text("Solar or fixed-local midday.", "חצות היום: שמשי או מקומי קבוע."), settings.chatzotMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Chatzot HaYom", "חצות היום"), ChatzotMethod.entries, settings.chatzotMethod, defaults.chatzotMethod, { it.localizedLabel(useHebrew) }, viewModel::setChatzotMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Mincha Gedola", "מנחה גדולה"), text("Earliest regular Mincha.", "הזמן המוקדם למנחה."), settings.minchaGedolaMethod.caption(settings, useHebrew)) {
        activePicker = picker(
            title = text("Mincha Gedola", "מנחה גדולה"),
            options = MinchaGedolaMethod.entries,
            selected = settings.minchaGedolaMethod,
            default = defaults.minchaGedolaMethod,
            label = { it.localizedLabel(useHebrew) },
            onSelect = viewModel::setMinchaGedolaMethod,
            customUnit = { it.customUnit },
            values = settings.minchaGedolaCustom,
            onSelectCustom = viewModel::setMinchaGedolaCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text("Mincha Ketana", "מנחה קטנה"), text("Preferred later Mincha window.", "תחילת זמן מנחה קטן."), settings.minchaKetanaMethod.caption(settings, useHebrew)) {
        activePicker = picker(
            title = text("Mincha Ketana", "מנחה קטנה"),
            options = MinchaKetanaMethod.entries,
            selected = settings.minchaKetanaMethod,
            default = defaults.minchaKetanaMethod,
            label = { it.localizedLabel(useHebrew) },
            onSelect = viewModel::setMinchaKetanaMethod,
            customUnit = { it.customUnit },
            values = settings.minchaKetanaCustom,
            onSelectCustom = viewModel::setMinchaKetanaCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text("Plag Hamincha", "פלג המנחה"), text("Earliest Shabbat or Maariv boundary.", "גבול מוקדם לקבלת שבת או מעריב."), settings.plagHaminchaMethod.caption(settings, useHebrew)) {
        activePicker = picker(
            title = text("Plag Hamincha", "פלג המנחה"),
            options = PlagHaminchaMethod.entries,
            selected = settings.plagHaminchaMethod,
            default = defaults.plagHaminchaMethod,
            label = { it.localizedLabel(useHebrew) },
            onSelect = viewModel::setPlagHaminchaMethod,
            customUnit = { it.customUnit },
            values = settings.plagHaminchaCustom,
            onSelectCustom = viewModel::setPlagHaminchaCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text("Sunset", "שקיעה"), text("Sea level or elevation-adjusted sunset.", "שקיעה במישור או מתוקנת לפי גובה."), settings.sunsetMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Sunset", "שקיעה"), SunsetMethod.entries, settings.sunsetMethod, defaults.sunsetMethod, { it.localizedLabel(useHebrew) }, viewModel::setSunsetMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Tzeit Hakochavim", "צאת הכוכבים"), text("Nightfall used for the app's Hebrew-date rollover.", "צאת הכוכבים שמשמש גם להחלפת תאריך עברי באפליקציה."), settings.tzeitHakochavimMethod.caption(settings, useHebrew)) {
        activePicker = picker(
            title = text("Tzeit Hakochavim", "צאת הכוכבים"),
            options = TzeitHakochavimMethod.entries,
            selected = settings.tzeitHakochavimMethod,
            default = defaults.tzeitHakochavimMethod,
            label = { it.localizedLabel(useHebrew) },
            onSelect = viewModel::setTzeitHakochavimMethod,
            customUnit = { it.customUnit },
            values = settings.tzeitHakochavimCustom,
            onSelectCustom = viewModel::setTzeitHakochavimCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(text("Chatzot HaLaila", "חצות הלילה"), text("Solar or fixed-local midnight.", "חצות הלילה: שמשי או מקומי קבוע."), settings.chatzotHaLailaMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Chatzot HaLaila", "חצות הלילה"), ChatzotMethod.entries, settings.chatzotHaLailaMethod, defaults.chatzotHaLailaMethod, { it.localizedLabel(useHebrew) }, viewModel::setChatzotHaLailaMethod)
    }
    SettingsDivider()
    // Candle lighting is the one picker whose fixed options are minhagim rather than rungs of a
    // calculation ladder — they are what the first-launch prompt offers — so all four stay, with a
    // typed-in value beside them.
    MethodChoiceRow(text("Candle Lighting", "הדלקת נרות"), text("Minutes before sunset for candle lighting.", "דקות לפני שקיעה להדלקת נרות."), settings.candleLightingMethod.caption(settings, useHebrew)) {
        activePicker = ZmanimMethodPicker(
            title = text("Candle Lighting", "הדלקת נרות"),
            options = CandleLightingMethod.entries.map { option ->
                val default = candleLightingDefault ?: defaults.candleLightingMethod
                val optionLabel = option.localizedLabel(useHebrew)
                ZmanimMethodOption(
                    label = when {
                        // Nothing to ask about the unit here — every option is a minute count — so
                        // the custom row leads straight to a number.
                        option.offsetMinutes == null -> text("Custom", "מותאם אישית")
                        option == default -> text("$optionLabel (default)", "$optionLabel (ברירת מחדל)")
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
    MethodChoiceRow(text("Motzei Shabbat", "צאת שבת"), text("Main end-of-Shabbat time.", "זמן צאת שבת הראשי."), settings.motzeiShabbatMethod.caption(settings, useHebrew)) {
        activePicker = picker(
            title = text("Motzei Shabbat", "צאת שבת"),
            options = MotzeiShabbatMethod.entries,
            selected = settings.motzeiShabbatMethod,
            default = defaults.motzeiShabbatMethod,
            label = { it.localizedLabel(useHebrew) },
            onSelect = viewModel::setMotzeiShabbatMethod,
            customUnit = { it.customUnit },
            values = settings.motzeiShabbatCustom,
            onSelectCustom = viewModel::setMotzeiShabbatCustomValue,
        )
    }
    SettingsDivider()
    MethodChoiceRow(
        text("Tosefet Shabbat / Yom Tov", "תוספת שבת/חג"),
        text("Minutes added after nightfall before Shabbat or Yom Tov goes out.", "דקות שמתווספות אחרי צאת הכוכבים ליציאת שבת או חג."),
        text("${settings.holyDayTosefetMinutes} minutes", "${settings.holyDayTosefetMinutes} דקות"),
    ) {
        showTosefetDialog = true
    }
    SettingsDivider()
    MethodChoiceRow(text("Rabbeinu Tam", "רבינו תם"), text("Separate Rabbeinu Tam Shabbat opinion.", "שיטת רבינו תם נפרדת לשבת."), settings.rabbeinuTamMethod.caption(settings, useHebrew)) {
        activePicker = picker(
            title = text("Rabbeinu Tam", "רבינו תם"),
            options = RabbeinuTamMethod.entries,
            selected = settings.rabbeinuTamMethod,
            default = defaults.rabbeinuTamMethod,
            label = { it.localizedLabel(useHebrew) },
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
            text("Ateret Torah — minutes after sunset", "עטרת תורה — דקות אחרי שקיעה"),
            text(
                "The end of the day every Ateret Torah opinion is measured to. 25 minutes in Israel, 40 elsewhere.",
                "סוף היום שאליו נמדדות כל שיטות עטרת תורה. 25 דקות בארץ, 40 בחו״ל.",
            ),
            text("${settings.ateretTorahSunsetOffsetMinutes} minutes", "${settings.ateretTorahSunsetOffsetMinutes} דקות"),
        ) {
            showAteretTorahDialog = true
        }
    }
    SettingsDivider()
    MethodChoiceRow(text("Erev Pesach Chametz", "חמץ בערב פסח"), text("Sof zman eating and burning chametz.", "סוף זמן אכילת חמץ וביעור חמץ."), settings.chametzMethod.caption(settings, useHebrew)) {
        activePicker = picker(
            title = text("Erev Pesach Chametz", "חמץ בערב פסח"),
            options = ChametzMethod.entries,
            selected = settings.chametzMethod,
            default = defaults.chametzMethod,
            label = { it.localizedLabel(useHebrew) },
            onSelect = viewModel::setChametzMethod,
            customUnit = { it.customUnit },
            values = settings.chametzCustom,
            onSelectCustom = viewModel::setChametzCustomValue,
        )
    }

    if (showTosefetDialog) {
        MinutesInputDialog(
            title = text("Tosefet Shabbat / Yom Tov", "תוספת שבת/חג"),
            description = text(
                "Minutes added after nightfall. The default is 5.",
                "דקות שמתווספות אחרי צאת הכוכבים. ברירת המחדל היא 5.",
            ),
            initialMinutes = settings.holyDayTosefetMinutes,
            confirmLabel = text("Save", "שמירה"),
            dismissLabel = text("Cancel", "ביטול"),
            onDismiss = { showTosefetDialog = false },
            onConfirm = { minutes ->
                viewModel.setHolyDayTosefetMinutes(minutes)
                showTosefetDialog = false
            },
        )
    }

    if (showAteretTorahDialog) {
        MinutesInputDialog(
            title = text("Ateret Torah", "עטרת תורה"),
            description = text(
                "Minutes after sunset. Chacham Harari-Raful gave 25 for Israel; 40 is used elsewhere.",
                "דקות אחרי שקיעה. חכם הררי-רפול נתן 25 דקות לארץ ישראל; בחו״ל נוהגים 40.",
            ),
            initialMinutes = settings.ateretTorahSunsetOffsetMinutes,
            confirmLabel = text("Save", "שמירה"),
            dismissLabel = text("Cancel", "ביטול"),
            onDismiss = { showAteretTorahDialog = false },
            onConfirm = { minutes ->
                viewModel.setAteretTorahSunsetOffsetMinutes(minutes)
                showAteretTorahDialog = false
            },
        )
    }

    if (showCandleLightingDialog) {
        MinutesInputDialog(
            title = text("Candle Lighting", "הדלקת נרות"),
            description = text(
                "Minutes before sunset.",
                "דקות לפני השקיעה.",
            ),
            initialMinutes = settings.candleLightingCustomMinutes,
            confirmLabel = text("Save", "שמירה"),
            dismissLabel = text("Cancel", "ביטול"),
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
            description = text(
                "How this zman is measured, and the value:",
                "איך הזמן נמדד, והערך:",
            ),
            confirmLabel = text("Save", "שמירה"),
            dismissLabel = text("Cancel", "ביטול"),
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
                    Text(localizedString(R.string.settings_cancel, R.string.settings_cancel_hebrew))
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