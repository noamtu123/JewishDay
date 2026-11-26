// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.model.CustomZmanUnit
import com.noamtu.jewishday.i18n.label

/**
 * The custom dialog: pick how the zman is measured, then enter the value. Both questions belong
 * together — a number means nothing without its unit — and this is the only place either is asked.
 *
 * Switching the unit re-fills the field from that unit's own stored number, so 16.1° and 72 minutes
 * both survive being looked at. Degrees take a decimal point (19.848° is a real opinion); the minute
 * units are whole minutes, the only precision they are ever stated in.
 */
@Composable
internal fun CustomMethodDialog(
    prompt: CustomMethodPrompt,
    description: String,
    confirmLabel: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (CustomMethodChoice, Double) -> Unit,
) {
    var selectedIndex by rememberSaveable(prompt.title) { mutableIntStateOf(prompt.initialIndex) }
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
        title = { Text(prompt.title, modifier = Modifier.semantics { heading() }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(description, style = MaterialTheme.typography.bodyMedium)
                // A zman measured only one way has nothing to choose, so it goes straight to the field.
                if (prompt.choices.size > 1) {
                    // The same spacing as the column around it, so grouping them changes nothing on screen.
                    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        prompt.choices.forEachIndexed { choiceIndex, option ->
                            ThemeOptionRow(
                                label = option.label,
                                selected = choiceIndex == index,
                                onClick = { selectedIndex = choiceIndex },
                            )
                        }
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
internal fun MinutesInputDialog(
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
    val fieldLabel = stringResource(R.string.a11y_minutes_field)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, modifier = Modifier.semantics { heading() }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(description, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    // No visible label (the dialog's text is the label), so TalkBack is told what
                    // the number is.
                    modifier = Modifier.semantics { contentDescription = fieldLabel },
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
