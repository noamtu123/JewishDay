// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.model.AlotHashacharMethod
import com.noamtu.jewishday.core.model.CandleLightingMethod
import com.noamtu.jewishday.core.model.ChametzMethod
import com.noamtu.jewishday.core.model.ChatzotMethod
import com.noamtu.jewishday.core.model.CustomZmanUnit
import com.noamtu.jewishday.core.model.CustomZmanValue
import com.noamtu.jewishday.core.model.MinchaGedolaMethod
import com.noamtu.jewishday.core.model.MinchaKetanaMethod
import com.noamtu.jewishday.core.model.MisheyakirMethod
import com.noamtu.jewishday.core.model.MotzeiShabbatMethod
import com.noamtu.jewishday.core.model.NoTzeitFallback
import com.noamtu.jewishday.core.model.PlagHaminchaMethod
import com.noamtu.jewishday.core.model.RabbeinuTamMethod
import com.noamtu.jewishday.core.model.SofZmanShemaMethod
import com.noamtu.jewishday.core.model.SofZmanTefillahMethod
import com.noamtu.jewishday.core.model.SunriseMethod
import com.noamtu.jewishday.core.model.SunsetMethod
import com.noamtu.jewishday.core.model.TzeitHakochavimMethod
import com.noamtu.jewishday.core.model.ZmanOpinionFamily
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.customUnit
import com.noamtu.jewishday.i18n.LocalTextResolver
import com.noamtu.jewishday.i18n.UiText
import com.noamtu.jewishday.i18n.caption
import com.noamtu.jewishday.i18n.label
import com.noamtu.jewishday.i18n.pluralText
import com.noamtu.jewishday.i18n.resolve
import com.noamtu.jewishday.i18n.verbatim

@Composable
internal fun AdvancedZmanimChoices(
    settings: ZmanimCalculationSettings,
    candleLightingDefault: CandleLightingMethod?,
    offersNoTzeitFallback: Boolean,
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
    fun text(@StringRes id: Int, vararg args: String): String = resolver.string(id, *args.map(::verbatim).toTypedArray())
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
    // Only where some night in the coming year has no tzeit by the method above: anywhere else the
    // choice would never change a thing, and only clutters the list.
    if (offersNoTzeitFallback) {
        SettingsDivider()
        MethodChoiceRow(text(R.string.settings_method_no_tzeit), text(R.string.settings_method_no_tzeit_description), settings.noTzeitFallback.label.text()) {
            activePicker = picker(
                text(R.string.settings_method_no_tzeit),
                NoTzeitFallback.entries,
                settings.noTzeitFallback,
                defaults.noTzeitFallback,
                { it.label.text() },
                viewModel::setNoTzeitFallback,
            )
        }
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
            title = { Text(pickerConfig.title, modifier = Modifier.semantics { heading() }) },
            text = {
                LazyColumn(
                    // Sized to its options (most zmanim have two or three), scrolling only past the cap.
                    modifier = Modifier.heightIn(max = 420.dp).selectableGroup(),
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
internal data class CustomMethodChoice(
    val label: String,
    val unit: CustomZmanUnit,
    val onConfirm: (Double) -> Unit,
)

/** An open "Custom" dialog: the ways this zman can be measured, and the numbers already held. */
internal data class CustomMethodPrompt(
    val title: String,
    val choices: List<CustomMethodChoice>,
    val initialIndex: Int,
    val values: CustomZmanValue,
)
