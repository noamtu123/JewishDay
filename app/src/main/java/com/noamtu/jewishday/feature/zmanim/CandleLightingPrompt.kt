// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.zmanim

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.model.CandleLightingMethod
import com.noamtu.jewishday.feature.settings.wholeMinutes
import com.noamtu.jewishday.ui.theme.glassBorder
import com.noamtu.jewishday.ui.theme.glassOr

@Composable
internal fun CandleLightingPrompt(
    onSelected: (CandleLightingMethod) -> Unit,
    onMinutesEntered: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomEntry by rememberSaveable { mutableStateOf(false) }
    var customMinutes by rememberSaveable { mutableStateOf("18") }
    Surface(
        modifier = modifier.clickable(role = Role.Button) { showDialog = true },
        shape = MaterialTheme.shapes.large,
        color = glassOr(MaterialTheme.colorScheme.secondaryContainer),
        border = glassBorder(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.zmanim_candle_prompt_question),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.zmanim_candle_prompt_choose),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(stringResource(R.string.zmanim_candle_prompt_title), modifier = Modifier.semantics { heading() })
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.zmanim_candle_prompt_body))
                    Spacer(Modifier.height(4.dp))
                    // The common minhagim only — the typed-in option belongs in Settings, not in a
                    // first-launch question.
                    CandleLightingMethod.PromptOptions.forEach { method ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showDialog = false
                                onSelected(method)
                            },
                        ) {
                            // Non-null for every prompt option, by construction.
                            val minutes = method.offsetMinutes ?: 18
                            Text(pluralStringResource(R.plurals.method_minutes_value, minutes, minutes))
                        }
                    }
                    // The three numbers cover most practices but not all — 18 minutes among them —
                    // so the same question also takes a typed-in answer.
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showDialog = false
                            showCustomEntry = true
                        },
                    ) {
                        Text(stringResource(R.string.zmanim_candle_prompt_custom))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
    if (showCustomEntry) {
        val minutes = wholeMinutes(customMinutes)
        val fieldLabel = stringResource(R.string.a11y_minutes_field)
        AlertDialog(
            onDismissRequest = { showCustomEntry = false },
            title = {
                Text(stringResource(R.string.zmanim_candle_prompt_title), modifier = Modifier.semantics { heading() })
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.zmanim_candle_prompt_body))
                    OutlinedTextField(
                        // The field has no visible label (the question above it is the label), so
                        // TalkBack is told what the number is.
                        modifier = Modifier.semantics { contentDescription = fieldLabel },
                        value = customMinutes,
                        onValueChange = { typed ->
                            // Any number: one earlier than plag hamincha is held at plag.
                            if (typed.all(Char::isDigit)) customMinutes = typed
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Text(
                        stringResource(R.string.settings_candle_lighting_plag_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = minutes != null,
                    onClick = {
                        minutes?.let(onMinutesEntered)
                        showCustomEntry = false
                    },
                ) {
                    Text(stringResource(R.string.zmanim_candle_prompt_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomEntry = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
}
