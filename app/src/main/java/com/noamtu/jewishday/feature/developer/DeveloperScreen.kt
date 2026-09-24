// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noamtu.jewishday.BuildConfig
import com.noamtu.jewishday.data.DeveloperLocationPresets
import com.noamtu.jewishday.data.developerLocationPreset
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
import com.noamtu.jewishday.ui.components.ScreenSurface
import com.noamtu.jewishday.ui.components.readableWidth
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperScreen(
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {},
    viewModel: DeveloperViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val overrides = state.overrides
    val context = LocalContext.current
    val updateCheckResult by viewModel.updateCheckResult.collectAsStateWithLifecycle()
    val skyTime by viewModel.skyPreviewTime.collectAsStateWithLifecycle()
    val skyPlaying by viewModel.skyPreviewPlaying.collectAsStateWithLifecycle()
    val skyZone = state.skyZone
    var showSpoofedVersionDialog by rememberSaveable { mutableStateOf(false) }
    var showDisableDialog by rememberSaveable { mutableStateOf(false) }

    ScreenSurface(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .readableWidth()
                .fillMaxSize(),
            contentPadding = ScreenPaddingValues,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Status first: the way out, what is in effect right now, and one tap to undo it all.
            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    SwitchRow(
                        label = "Developer mode",
                        checked = true,
                        onCheckedChange = { showDisableDialog = true },
                    )
                    Spacer(Modifier.height(8.dp))
                    ReadoutRow("Now", state.effectiveDateTime)
                    ReadoutRow("Hebrew date", state.jewishDate + if (state.dayInfo != "Regular day") " · ${state.dayInfo}" else "")
                    ReadoutRow("Location", state.effectiveLocation + if (state.inIsrael) " · Israel" else " · Diaspora")
                    val active = activeOverrides(overrides)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (active.isEmpty()) "No overrides active" else "Active: " + active.joinToString(" · "),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (active.isEmpty()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                        if (active.isNotEmpty()) {
                            TextButton(onClick = viewModel::resetOverrides) { Text("Reset all") }
                        }
                    }
                }
            }

            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("Clock")
                    SwitchRow(
                        label = "Override the clock",
                        checked = overrides.timeOverrideEnabled,
                        onCheckedChange = viewModel::setTimeOverrideEnabled,
                    )
                    if (overrides.timeOverrideEnabled) {
                        SwitchRow(
                            label = "Freeze time",
                            checked = overrides.timeFrozen,
                            onCheckedChange = viewModel::setTimeFrozen,
                        )
                        SwitchRow(
                            label = "Hide the warning banner",
                            checked = overrides.hideTimeOverrideBanner,
                            onCheckedChange = viewModel::setHideTimeOverrideBanner,
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.shiftDays(-1) }) { Text("-1 day") }
                            OutlinedButton(onClick = { viewModel.shiftDays(1) }) { Text("+1 day") }
                            OutlinedButton(onClick = { viewModel.shiftHours(-1) }) { Text("-1 hour") }
                            OutlinedButton(onClick = { viewModel.shiftHours(1) }) { Text("+1 hour") }
                            // Both pickers open on the date and time being simulated, not on the
                            // real now — coming back to this screen mid-spoof used to offer to
                            // reset you to today, which is never what was wanted.
                            OutlinedButton(
                                onClick = {
                                    val date = state.effectiveDate
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            viewModel.setOverrideDate(LocalDate.of(year, month + 1, dayOfMonth))
                                        },
                                        date.year,
                                        date.monthValue - 1,
                                        date.dayOfMonth,
                                    ).show()
                                },
                            ) { Text("Pick date…") }
                            OutlinedButton(
                                onClick = {
                                    val time = state.effectiveTime
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            viewModel.setOverrideTime(LocalTime.of(hourOfDay, minute))
                                        },
                                        time.hour,
                                        time.minute,
                                        true,
                                    ).show()
                                },
                            ) { Text("Pick time…") }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Jump to the next…",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeveloperJumpTarget.entries.forEach { target ->
                            AssistChip(
                                onClick = { viewModel.jumpTo(target) },
                                label = { Text(target.label) },
                            )
                        }
                    }
                }
            }

            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("Sky theme")
                    // Moves the Sky theme's backdrop alone; the zmanim stay on the real clock.
                    val time = skyTime
                    val minute = time?.let { it.hour * 60 + it.minute } ?: state.effectiveTime.let { it.hour * 60 + it.minute }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (time == null) "Following the clock" else "Sky at %02d:%02d".format(time.hour, time.minute),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (time != null) {
                            TextButton(onClick = viewModel::followClockWithSky) { Text("Back to clock") }
                        }
                    }
                    Slider(
                        value = minute.toFloat(),
                        onValueChange = { viewModel.showSkyAt(LocalTime.of(it.toInt() / 60, it.toInt() % 60)) },
                        valueRange = 0f..1439f,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = if (skyPlaying) viewModel::stopSkyDay else viewModel::playSkyDay) {
                            Text(if (skyPlaying) "Pause" else "Play the day")
                        }
                        state.skyPhases.forEach { phase ->
                            AssistChip(
                                onClick = { viewModel.showSkyAt(phase.at.atZone(skyZone).toLocalTime()) },
                                label = { Text("${phase.name} ${"%02d:%02d".format(phase.at.atZone(skyZone).hour, phase.at.atZone(skyZone).minute)}") },
                            )
                        }
                    }
                }
            }

            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("Location")
                    SwitchRow(
                        label = "Pin location",
                        checked = overrides.locationOverrideEnabled,
                        onCheckedChange = viewModel::setLocationOverrideEnabled,
                    )
                    if (overrides.locationOverrideEnabled) {
                        Spacer(Modifier.height(4.dp))
                        LocationPresetPicker(
                            selectedId = overrides.locationPresetId,
                            onSelect = viewModel::setLocationPreset,
                        )
                    }
                }
            }

            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("Language")
                    SwitchRow(
                        label = "About page in English",
                        checked = overrides.aboutInEnglish,
                        onCheckedChange = viewModel::setAboutInEnglish,
                    )
                    SwitchRow(
                        label = "Update changelog in English",
                        checked = overrides.updateNotesInEnglish,
                        onCheckedChange = viewModel::setUpdateNotesInEnglish,
                    )
                }
            }

            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("App updates")
                    ValueRow(
                        label = "Pretend to be version",
                        value = overrides.spoofedVersionName.ifBlank { "Real (${BuildConfig.VERSION_NAME})" },
                        onClick = { showSpoofedVersionDialog = true },
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = viewModel::runUpdateCheck) { Text("Run update check") }
                    updateCheckResult?.let { result ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("Diagnostics")
                    SwitchRow(
                        label = "Monitor compass sensors",
                        checked = overrides.compassMonitoringEnabled,
                        onCheckedChange = viewModel::setCompassMonitoringEnabled,
                    )
                }
            }
        }
    }

    if (showDisableDialog) {
        AlertDialog(
            onDismissRequest = { showDisableDialog = false },
            title = { Text("Turn off developer mode?") },
            text = {
                Text(
                    "The clock, location and version overrides are all cleared, and the tools " +
                        "disappear until the version is tapped 7× again.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisableDialog = false
                        viewModel.disableDeveloperMode()
                        // The screen it is on is about to stop existing, so leave it.
                        onExit()
                    },
                ) { Text("Turn off") }
            },
            dismissButton = {
                TextButton(onClick = { showDisableDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showSpoofedVersionDialog) {
        SpoofedVersionDialog(
            initialValue = overrides.spoofedVersionName,
            realVersion = BuildConfig.VERSION_NAME,
            onDismiss = { showSpoofedVersionDialog = false },
            onConfirm = { entered ->
                viewModel.setSpoofedVersionName(entered)
                showSpoofedVersionDialog = false
            },
        )
    }
}

@Composable
private fun LocationPresetPicker(
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = developerLocationPreset(selectedId)?.displayName ?: "Choose a city"
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(selectedName) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DeveloperLocationPresets.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.displayName) },
                    onClick = {
                        onSelect(preset.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** What is being overridden right now, in a word each, for the status card. */
private fun activeOverrides(overrides: com.noamtu.jewishday.data.DeveloperOverrides): List<String> = buildList {
    if (overrides.timeOverrideEnabled) add(if (overrides.timeFrozen) "Clock (frozen)" else "Clock")
    if (overrides.locationOverrideEnabled) add(developerLocationPreset(overrides.locationPresetId)?.displayName ?: "Location")
    if (overrides.spoofedVersionName.isNotBlank()) add("Version ${overrides.spoofedVersionName}")
    if (overrides.aboutInEnglish || overrides.updateNotesInEnglish) add("English")
    if (overrides.compassMonitoringEnabled) add("Compass monitor")
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ReadoutRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = label,
            modifier = Modifier.width(110.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** A tappable "setting" row: its label, the value it currently holds, and a tap to change it. */
@Composable
private fun ValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Asks for the version the update check should compare against, and only stores it on Save — so
 * the row behind it changes exactly once, when you meant it to, instead of on every keystroke.
 */
@Composable
private fun SpoofedVersionDialog(
    initialValue: String,
    realVersion: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pretend this build is version") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "An X.Y.Z version. Anything on GitHub newer than it is offered as an " +
                        "update. Empty means the real version, $realVersion.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { entered -> text = entered },
                    singleLine = true,
                    placeholder = { Text(realVersion) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
