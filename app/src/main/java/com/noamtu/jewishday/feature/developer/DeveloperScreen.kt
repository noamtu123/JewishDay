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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noamtu.jewishday.data.DeveloperLocationPresets
import com.noamtu.jewishday.data.developerLocationPreset
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
import com.noamtu.jewishday.ui.components.ScreenSurface
import com.noamtu.jewishday.ui.components.readableWidth
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperScreen(
    modifier: Modifier = Modifier,
    viewModel: DeveloperViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val overrides = state.overrides
    val context = LocalContext.current

    ScreenSurface(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .readableWidth()
                .fillMaxSize(),
            contentPadding = ScreenPaddingValues,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("Effective state")
                    ReadoutRow("Date & time", state.effectiveDateTime)
                    ReadoutRow("Jewish date", state.jewishDate)
                    ReadoutRow("Today is", state.dayInfo)
                    ReadoutRow("Location", state.effectiveLocation)
                    ReadoutRow("In Israel", if (state.inIsrael) "Yes" else "No")
                }
            }

            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("Date & time override")
                    SwitchRow(
                        label = "Override the clock",
                        checked = overrides.timeOverrideEnabled,
                        onCheckedChange = viewModel::setTimeOverrideEnabled,
                    )
                    if (overrides.timeOverrideEnabled) {
                        SwitchRow(
                            label = "Freeze time (otherwise it keeps ticking)",
                            checked = overrides.timeFrozen,
                            onCheckedChange = viewModel::setTimeFrozen,
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.shiftDays(-1) }) { Text("-1 day") }
                            OutlinedButton(onClick = { viewModel.shiftDays(1) }) { Text("+1 day") }
                            OutlinedButton(onClick = { viewModel.shiftHours(-1) }) { Text("-1 hour") }
                            OutlinedButton(onClick = { viewModel.shiftHours(1) }) { Text("+1 hour") }
                            OutlinedButton(
                                onClick = {
                                    val today = LocalDate.now()
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            viewModel.setOverrideDate(LocalDate.of(year, month + 1, dayOfMonth))
                                        },
                                        today.year,
                                        today.monthValue - 1,
                                        today.dayOfMonth,
                                    ).show()
                                },
                            ) { Text("Pick date…") }
                        }
                    }
                }
            }

            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("Jump to next…")
                    Text(
                        text = "Sets the clock to the next occurrence, so you can test that day's behavior.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
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
                    SectionTitle("Location override")
                    SwitchRow(
                        label = "Pin location",
                        checked = overrides.locationOverrideEnabled,
                        onCheckedChange = viewModel::setLocationOverrideEnabled,
                    )
                    if (overrides.locationOverrideEnabled) {
                        Spacer(Modifier.height(8.dp))
                        LocationPresetPicker(
                            selectedId = overrides.locationPresetId,
                            onSelect = viewModel::setLocationPreset,
                        )
                    }
                    // "In Israel" is derived from the effective location (see the readout above);
                    // pick a diaspora location preset to exercise two-day Yom Tov.
                }
            }

            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("About page")
                    SwitchRow(
                        label = "Show About page in English",
                        checked = overrides.aboutInEnglish,
                        onCheckedChange = viewModel::setAboutInEnglish,
                    )
                }
            }

            item {
                Button(
                    onClick = viewModel::resetOverrides,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reset all overrides")
                }
            }
        }
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