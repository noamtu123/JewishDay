// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.data.DeveloperLocationPresets
import com.noamtu.jewishday.data.developerLocationPreset

@Composable
internal fun LocationPresetPicker(
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

/**
 * Asks for the version the update check should compare against, and only stores it on Save — so
 * the row behind it changes exactly once, when you meant it to, instead of on every keystroke.
 */
@Composable
internal fun SpoofedVersionDialog(
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
