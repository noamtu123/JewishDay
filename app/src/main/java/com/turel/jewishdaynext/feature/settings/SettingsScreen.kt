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
import com.turel.jewishdaynext.R
import com.turel.jewishdaynext.data.AppThemeOption
import com.turel.jewishdaynext.model.*
import com.turel.jewishdaynext.ui.components.InfoCard
import com.turel.jewishdaynext.ui.components.ScreenPaddingValues
import com.turel.jewishdaynext.ui.components.ScreenSurface
import com.turel.jewishdaynext.ui.components.readableWidth
import com.turel.jewishdaynext.ui.localizedString

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingNotificationTarget by remember { mutableStateOf<NotificationPermissionTarget?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showZmanimPresetDialog by remember { mutableStateOf(false) }
    var showDetailedCalculationMethods by remember { mutableStateOf(false) }
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
                    SettingsChoiceRow(
                        label = localizedString(R.string.settings_theme, R.string.settings_theme_hebrew),
                        value = uiState.themeOption.localizedLabel(),
                        onClick = { showThemeDialog = true },
                    )
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
                    SettingsChoiceRow(
                        label = localizedString(R.string.settings_zmanim_preset, R.string.settings_zmanim_preset_hebrew),
                        description = uiState.zmanimSettings.preset.description,
                        value = uiState.zmanimSettings.preset.localizedLabel(),
                        onClick = { showZmanimPresetDialog = true },
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
                        label = localizedString(R.string.settings_advanced_zmanim, R.string.settings_advanced_zmanim_hebrew),
                        description = localizedString(R.string.settings_advanced_zmanim_description, R.string.settings_advanced_zmanim_description_hebrew),
                        checked = showDetailedCalculationMethods,
                        onCheckedChange = { showDetailedCalculationMethods = it },
                    )
                    if (showDetailedCalculationMethods) {
                        val zmanim = uiState.zmanimSettings
                        SettingsDivider()
                        AdvancedZmanimChoices(
                            settings = zmanim,
                            viewModel = viewModel,
                        )
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
                    Text(localizedString(R.string.locations_cancel, R.string.locations_cancel_hebrew))
                }
            },
        )
    }

    if (showZmanimPresetDialog) {
        AlertDialog(
            onDismissRequest = { showZmanimPresetDialog = false },
            title = { Text(localizedString(R.string.settings_zmanim_preset, R.string.settings_zmanim_preset_hebrew)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ZmanimPreset.entries.forEach { preset ->
                        ThemeOptionRow(
                            label = preset.localizedLabel(),
                            selected = preset == uiState.zmanimSettings.preset,
                            onClick = {
                                viewModel.setZmanimPreset(preset)
                                showZmanimPresetDialog = false
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showZmanimPresetDialog = false }) {
                    Text(localizedString(R.string.locations_cancel, R.string.locations_cancel_hebrew))
                }
            },
        )
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
    AppThemeOption.IsraelSky -> localizedString(R.string.theme_israel_sky, R.string.theme_israel_sky_hebrew)
    AppThemeOption.JerusalemStone -> localizedString(R.string.theme_jerusalem_stone, R.string.theme_jerusalem_stone_hebrew)
    AppThemeOption.AmoledBlack -> localizedString(R.string.theme_amoled_black, R.string.theme_amoled_black_hebrew)
}

@Composable
private fun ZmanimPreset.localizedLabel(): String = if (com.turel.jewishdaynext.ui.LocalUseHebrewInterface.current) labelHebrew else label

private fun HighLatitudeHandling.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun AlotHashacharMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun MisheyakirMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun SunriseMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun SofZmanShemaMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun SofZmanTefillahMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun ChatzotMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun MinchaGedolaMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
private fun SamuchLeMinchaKetanaMethod.localizedLabel(useHebrew: Boolean): String = if (useHebrew) labelHebrew else label
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
    val useHebrew = com.turel.jewishdaynext.ui.LocalUseHebrewInterface.current
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
    MethodChoiceRow(text("Misheyakir", "משיכיר"), text("Earliest tallit and tefillin time.", "זמן מוקדם לטלית ותפילין."), settings.misheyakirMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Misheyakir", "משיכיר"), MisheyakirMethod.entries, settings.misheyakirMethod, { it.localizedLabel(useHebrew) }, viewModel::setMisheyakirMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Sunrise", "הנץ החמה"), text("Sea level is the common zmanim base; observed uses elevation.", "מישור הוא בסיס נפוץ לזמנים; נראית משתמשת בגובה."), settings.sunriseMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Sunrise", "הנץ החמה"), SunriseMethod.entries, settings.sunriseMethod, { it.localizedLabel(useHebrew) }, viewModel::setSunriseMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Shema", "קריאת שמע"), text("Latest Shema method shown in the main Zmanim list.", "שיטת סוף זמן שמע המוצגת ברשימת הזמנים."), settings.sofZmanShemaMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Shema", "קריאת שמע"), SofZmanShemaMethod.entries, settings.sofZmanShemaMethod, { it.localizedLabel(useHebrew) }, viewModel::setSofZmanShemaMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Tefillah", "תפילה"), text("Latest Shacharit method shown in the main Zmanim list.", "שיטת סוף זמן תפילה המוצגת ברשימת הזמנים."), settings.sofZmanTefillahMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Tefillah", "תפילה"), SofZmanTefillahMethod.entries, settings.sofZmanTefillahMethod, { it.localizedLabel(useHebrew) }, viewModel::setSofZmanTefillahMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Chatzot", "חצות"), text("Solar midday or fixed-local calculation.", "חצות שמשי או חצות מקומי קבוע."), settings.chatzotMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Chatzot", "חצות"), ChatzotMethod.entries, settings.chatzotMethod, { it.localizedLabel(useHebrew) }, viewModel::setChatzotMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Mincha Gedola", "מנחה גדולה"), text("Earliest regular Mincha.", "הזמן המוקדם למנחה."), settings.minchaGedolaMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Mincha Gedola", "מנחה גדולה"), MinchaGedolaMethod.entries, settings.minchaGedolaMethod, { it.localizedLabel(useHebrew) }, viewModel::setMinchaGedolaMethod)
    }
    SettingsDivider()
    MethodChoiceRow(text("Samuch LeMincha Ketana", "סמוך למנחה קטנה"), text("Half hour before Mincha Ketana or equivalent zmanis method.", "חצי שעה לפני מנחה קטנה או שיטה זמנית מקבילה."), settings.samuchLeMinchaKetanaMethod.localizedLabel(useHebrew)) {
        activePicker = picker(text("Samuch LeMincha Ketana", "סמוך למנחה קטנה"), SamuchLeMinchaKetanaMethod.entries, settings.samuchLeMinchaKetanaMethod, { it.localizedLabel(useHebrew) }, viewModel::setSamuchLeMinchaKetanaMethod)
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
                    Text(localizedString(R.string.locations_cancel, R.string.locations_cancel_hebrew))
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
