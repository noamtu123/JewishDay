package com.noamtu.jewishday.feature.settings

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showZmanimTimes by remember { mutableStateOf(false) }
    var showDailyLearning by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            when (pendingNotificationTarget) {
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
                    SettingsChoiceRow(
                        label = localizedString(R.string.settings_language, R.string.settings_language_hebrew),
                        description = localizedString(
                            R.string.settings_language_description,
                            R.string.settings_language_description_hebrew,
                        ),
                        value = uiState.language.displayName,
                        onClick = { showLanguageDialog = true },
                    )
                    SettingsDivider()
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
                    SettingsDivider()
                    SettingsChoiceRow(
                        label = localizedString(R.string.settings_theme, R.string.settings_theme_hebrew),
                        value = uiState.themeOption.localizedLabel(),
                        onClick = { showThemeDialog = true },
                    )
                }
            }
            item {
                SettingsSectionTitle(
                    text = localizedString(R.string.settings_zmanim_options, R.string.settings_zmanim_options_hebrew),
                )
            }
            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_outside_israel, R.string.settings_outside_israel_hebrew),
                        description = localizedString(R.string.settings_outside_israel_description, R.string.settings_outside_israel_description_hebrew),
                        // Default is Israel (inIsrael = true). The switch is framed as the
                        // diaspora opt-out, so its checked state is the inverse of inIsrael.
                        checked = !uiState.zmanimSettings.inIsrael,
                        onCheckedChange = { outsideIsrael -> viewModel.setInIsrael(!outsideIsrael) },
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = localizedString(R.string.settings_advanced_zmanim, R.string.settings_advanced_zmanim_hebrew),
                        description = localizedString(R.string.settings_advanced_zmanim_description, R.string.settings_advanced_zmanim_description_hebrew),
                        checked = uiState.advancedZmanimModeEnabled,
                        onCheckedChange = viewModel::setAdvancedZmanimModeEnabled,
                    )
                    if (uiState.advancedZmanimModeEnabled) {
                        val zmanim = uiState.zmanimSettings
                        SettingsDivider()
                        AdvancedZmanimChoices(
                            settings = zmanim,
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
                        expanded = showZmanimTimes,
                        onClick = { showZmanimTimes = !showZmanimTimes },
                    )
                    if (showZmanimTimes) {
                        ZmanimTimeOption.entries.forEach { option ->
                            SettingsDivider()
                            SettingsSwitchRow(
                                label = if (useHebrew) option.labelHebrew else option.labelEnglish,
                                description = "",
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
                        expanded = showDailyLearning,
                        onClick = { showDailyLearning = !showDailyLearning },
                    )
                    if (showDailyLearning) {
                        DailyLearningType.entries.forEach { type ->
                            SettingsDivider()
                            SettingsSwitchRow(
                                label = if (useHebrew) type.labelHebrew else type.labelEnglish,
                                description = "",
                                checked = type in uiState.enabledDailyLearning,
                                onCheckedChange = { enabled -> viewModel.setDailyLearningEnabled(type, enabled) },
                            )
                            if (type == DailyLearningType.RambamYomi) {
                                SettingsDivider()
                                SettingsSwitchRow(
                                    label = localizedString(R.string.settings_rambam_3_chapters, R.string.settings_rambam_3_chapters_hebrew),
                                    description = localizedString(R.string.settings_rambam_3_chapters_description, R.string.settings_rambam_3_chapters_description_hebrew),
                                    checked = uiState.rambamThreeChaptersEnabled,
                                    onCheckedChange = viewModel::setRambamThreeChaptersEnabled,
                                )
                            }
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

@Composable
private fun ExpandableSettingsHeader(
    label: String,
    description: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsChoiceRow(
        label = label,
        description = description,
        value = if (expanded) "-" else "+",
        onClick = onClick,
        modifier = modifier,
    )
}

private enum class NotificationPermissionTarget {
    HebrewStatusIcon,
    EnglishStatusIcon,
}

@Composable
private fun SettingsDivider(modifier: Modifier = Modifier) {
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
    description: String = "",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun AppThemeOption.localizedLabel(): String = when (this) {
    AppThemeOption.Classic -> localizedString(R.string.theme_classic, R.string.theme_classic_hebrew)
    AppThemeOption.BlueWhite -> localizedString(R.string.theme_blue_white, R.string.theme_blue_white_hebrew)
    AppThemeOption.JerusalemStone -> localizedString(R.string.theme_jerusalem_stone, R.string.theme_jerusalem_stone_hebrew)
    AppThemeOption.Sand -> localizedString(R.string.theme_sand, R.string.theme_sand_hebrew)
    AppThemeOption.Midnight -> localizedString(R.string.theme_midnight, R.string.theme_midnight_hebrew)
    AppThemeOption.Slate -> localizedString(R.string.theme_slate, R.string.theme_slate_hebrew)
    AppThemeOption.AmoledBlack -> localizedString(R.string.theme_amoled_black, R.string.theme_amoled_black_hebrew)
}

private fun HighLatitudeHandling.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
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
private fun BainHashmashotMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun FastDayMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun ChametzMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label

@Composable
private fun AdvancedZmanimChoices(
    settings: ZmanimCalculationSettings,
    viewModel: SettingsViewModel,
) {
    val useHebrew = LocalUseHebrewInterface.current
    var activePicker by remember { mutableStateOf<ZmanimMethodPicker?>(null) }

    fun text(english: String, hebrew: String): String = if (useHebrew) hebrew else english

    fun <T> picker(
        title: String,
        options: List<T>,
        selected: T,
        label: (T) -> String,
        onSelect: (T) -> Unit,
    ): ZmanimMethodPicker = ZmanimMethodPicker(
        title = title,
        options = options.map { option ->
            ZmanimMethodOption(
                label = label(option),
                selected = option == selected,
                onSelect = { onSelect(option) },
            )
        },
    )

    MethodChoiceRow(
        title = text("High Latitude", "קו רוחב גבוה"),
        description = text("Fallback behavior when sun-angle methods cannot be calculated.", "התנהגות כאשר שיטות מעלות אינן ניתנות לחישוב."),
        value = settings.highLatitudeHandling.localizedLabel(useHebrew),
        onClick = { activePicker = picker(text("High Latitude", "קו רוחב גבוה"), HighLatitudeHandling.entries, settings.highLatitudeHandling, { it.localizedLabel(useHebrew) }, viewModel::setHighLatitudeHandling) },
    )
    SettingsDivider()
    MethodChoiceRow(text("Alot Hashachar", "עלות השחר"), text("Dawn start used for Magen Avraham and fast days.", "תחילת היום למגן אברהם ולתעניות."), settings.alotHashacharMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Alot Hashachar", "עלות השחר"), AlotHashacharMethod.entries, settings.alotHashacharMethod, { it.localizedLabel(useHebrew) }, viewModel::setAlotHashacharMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Tallit & Tefillin", "זמן טלית ותפילין"), text("Earliest tallit and tefillin time (misheyakir).", "הזמן המוקדם לטלית ותפילין (משיכיר)."), settings.misheyakirMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Tallit & Tefillin", "זמן טלית ותפילין"), MisheyakirMethod.entries, settings.misheyakirMethod, { it.localizedLabel(useHebrew) }, viewModel::setMisheyakirMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Sunrise", "הנץ החמה"), text("Sea level is the common zmanim base; observed uses elevation.", "מישור הוא בסיס נפוץ לזמנים; נראית משתמשת בגובה."), settings.sunriseMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Sunrise", "הנץ החמה"), SunriseMethod.entries, settings.sunriseMethod, { it.localizedLabel(useHebrew) }, viewModel::setSunriseMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Sof Zman Shema (GRA)", "סוף זמן קריאת שמע (גר״א)"), text("Method for the GRA Shema row.", "השיטה לשורת קריאת שמע של הגר״א."), settings.sofZmanShemaGraMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Sof Zman Shema (GRA)", "סוף זמן קריאת שמע (גר״א)"), SofZmanShemaMethod.entries.filter { it.family == ZmanOpinionFamily.Gra }, settings.sofZmanShemaGraMethod, { it.localizedLabel(useHebrew) }, viewModel::setSofZmanShemaGraMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Sof Zman Shema (Magen Avraham)", "סוף זמן קריאת שמע (מגן אברהם)"), text("Method for the Magen Avraham Shema row.", "השיטה לשורת קריאת שמע של מגן אברהם."), settings.sofZmanShemaMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Sof Zman Shema (Magen Avraham)", "סוף זמן קריאת שמע (מגן אברהם)"), SofZmanShemaMethod.entries.filter { it.family == ZmanOpinionFamily.MagenAvraham }, settings.sofZmanShemaMethod, { it.localizedLabel(useHebrew) }, viewModel::setSofZmanShemaMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Sof Zman Tefillah (GRA)", "סוף זמן תפילה (גר״א)"), text("Method for the GRA Tefillah row.", "השיטה לשורת תפילה של הגר״א."), settings.sofZmanTefillahGraMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Sof Zman Tefillah (GRA)", "סוף זמן תפילה (גר״א)"), SofZmanTefillahMethod.entries.filter { it.family == ZmanOpinionFamily.Gra }, settings.sofZmanTefillahGraMethod, { it.localizedLabel(useHebrew) }, viewModel::setSofZmanTefillahGraMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Sof Zman Tefillah (Magen Avraham)", "סוף זמן תפילה (מגן אברהם)"), text("Method for the Magen Avraham Tefillah row.", "השיטה לשורת תפילה של מגן אברהם."), settings.sofZmanTefillahMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Sof Zman Tefillah (Magen Avraham)", "סוף זמן תפילה (מגן אברהם)"), SofZmanTefillahMethod.entries.filter { it.family == ZmanOpinionFamily.MagenAvraham }, settings.sofZmanTefillahMethod, { it.localizedLabel(useHebrew) }, viewModel::setSofZmanTefillahMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Chatzot HaYom", "חצות היום"), text("Solar or fixed-local midday.", "חצות היום: שמשי או מקומי קבוע."), settings.chatzotMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Chatzot HaYom", "חצות היום"), ChatzotMethod.entries, settings.chatzotMethod, { it.localizedLabel(useHebrew) }, viewModel::setChatzotMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Chatzot HaLaila", "חצות הלילה"), text("Solar or fixed-local midnight.", "חצות הלילה: שמשי או מקומי קבוע."), settings.chatzotHaLailaMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Chatzot HaLaila", "חצות הלילה"), ChatzotMethod.entries, settings.chatzotHaLailaMethod, { it.localizedLabel(useHebrew) }, viewModel::setChatzotHaLailaMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Mincha Gedola", "מנחה גדולה"), text("Earliest regular Mincha.", "הזמן המוקדם למנחה."), settings.minchaGedolaMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Mincha Gedola", "מנחה גדולה"), MinchaGedolaMethod.entries, settings.minchaGedolaMethod, { it.localizedLabel(useHebrew) }, viewModel::setMinchaGedolaMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Mincha Ketana", "מנחה קטנה"), text("Preferred later Mincha window.", "תחילת זמן מנחה קטן."), settings.minchaKetanaMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Mincha Ketana", "מנחה קטנה"), MinchaKetanaMethod.entries, settings.minchaKetanaMethod, { it.localizedLabel(useHebrew) }, viewModel::setMinchaKetanaMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Plag Hamincha", "פלג המנחה"), text("Earliest Shabbat or Maariv boundary.", "גבול מוקדם לקבלת שבת או מעריב."), settings.plagHaminchaMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Plag Hamincha", "פלג המנחה"), PlagHaminchaMethod.entries, settings.plagHaminchaMethod, { it.localizedLabel(useHebrew) }, viewModel::setPlagHaminchaMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Sunset", "שקיעה"), text("Sea level or elevation-adjusted sunset.", "שקיעה במישור או מתוקנת לפי גובה."), settings.sunsetMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Sunset", "שקיעה"), SunsetMethod.entries, settings.sunsetMethod, { it.localizedLabel(useHebrew) }, viewModel::setSunsetMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Tzeit Hakochavim", "צאת הכוכבים"), text("Nightfall used for the app's Hebrew-date rollover.", "צאת הכוכבים שמשמש גם להחלפת תאריך עברי באפליקציה."), settings.tzeitHakochavimMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Tzeit Hakochavim", "צאת הכוכבים"), TzeitHakochavimMethod.entries, settings.tzeitHakochavimMethod, { it.localizedLabel(useHebrew) }, viewModel::setTzeitHakochavimMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Candle Lighting", "הדלקת נרות"), text("Minutes before sunset for candle lighting.", "דקות לפני שקיעה להדלקת נרות."), settings.candleLightingMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Candle Lighting", "הדלקת נרות"), CandleLightingMethod.entries, settings.candleLightingMethod, { it.localizedLabel(useHebrew) }, viewModel::setCandleLightingMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Motzei Shabbat", "צאת שבת"), text("Main end-of-Shabbat time.", "זמן צאת שבת הראשי."), settings.motzeiShabbatMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Motzei Shabbat", "צאת שבת"), MotzeiShabbatMethod.entries, settings.motzeiShabbatMethod, { it.localizedLabel(useHebrew) }, viewModel::setMotzeiShabbatMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Rabbeinu Tam", "רבינו תם"), text("Separate Rabbeinu Tam Shabbat opinion.", "שיטת רבינו תם נפרדת לשבת."), settings.rabbeinuTamMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Rabbeinu Tam", "רבינו תם"), RabbeinuTamMethod.entries, settings.rabbeinuTamMethod, { it.localizedLabel(useHebrew) }, viewModel::setRabbeinuTamMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Bain Hashmashot", "בין השמשות"), text("Twilight boundary method shown with Shabbat times.", "שיטת בין השמשות המוצגת עם זמני שבת."), settings.bainHashmashotMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Bain Hashmashot", "בין השמשות"), BainHashmashotMethod.entries, settings.bainHashmashotMethod, { it.localizedLabel(useHebrew) }, viewModel::setBainHashmashotMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Fast Days", "תעניות"), text("Start/end method for public fasts.", "שיטת התחלה וסיום לתעניות ציבור."), settings.fastDayMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Fast Days", "תעניות"), FastDayMethod.entries, settings.fastDayMethod, { it.localizedLabel(useHebrew) }, viewModel::setFastDayMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Erev Pesach Chametz", "חמץ בערב פסח"), text("Sof zman eating and burning chametz.", "סוף זמן אכילת חמץ וביעור חמץ."), settings.chametzMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Erev Pesach Chametz", "חמץ בערב פסח"), ChametzMethod.entries, settings.chametzMethod, { it.localizedLabel(useHebrew) }, viewModel::setChametzMethod)
    }

    activePicker?.let { pickerConfig ->
        AlertDialog(
            onDismissRequest = { activePicker = null },
            title = { Text(pickerConfig.title) },
            text = {
                LazyColumn(
                    modifier = Modifier.height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(pickerConfig.options.size) { index ->
                        val option = pickerConfig.options[index]
                        ThemeOptionRow(
                            label = option.label,
                            selected = option.selected,
                            onClick = {
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
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
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
