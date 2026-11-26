// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.R
import com.noamtu.jewishday.i18n.label

/**
 * Header for an expandable settings section: tapping the label area expands/collapses it, and a
 * circular reset icon on the side restores that section to its defaults (this replaces the old
 * +/- indicator on every expandable header).
 */
@Composable
internal fun ExpandableSettingsHeader(
    label: String,
    description: String,
    expanded: Boolean,
    resetContentDescription: String,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateText = stringResource(if (expanded) R.string.a11y_expanded else R.string.a11y_collapsed)
    val actionText = stringResource(if (expanded) R.string.a11y_action_collapse else R.string.a11y_action_expand)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            // Nothing on screen says whether the section is open but the rows under it, so TalkBack
            // is told: "Zmanim to show, collapsed, double-tap to expand". A heading, since it heads
            // its section.
            modifier = Modifier
                .weight(1f)
                .clickable(onClickLabel = actionText, role = Role.Button, onClick = onToggle)
                .semantics {
                    stateDescription = stateText
                    heading()
                },
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

@Composable
internal fun SettingsDivider(modifier: Modifier = Modifier) {
    // Hairline divider with no padding of its own — the rows' vertical padding supplies the
    // spacing. Keeps the long expandable zmanim sections compact and quick to scroll.
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp,
    )
}

@Composable
internal fun SectionSettingsDivider(modifier: Modifier = Modifier) {
    // The original roomier divider, used only in the top settings card (language, theme, …)
    // where there are few rows and the extra breathing room reads better.
    HorizontalDivider(modifier = modifier.padding(vertical = 12.dp))
}

@Composable
internal fun SettingsSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
            .semantics { heading() },
    )
}

@Composable
internal fun SettingsHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_summary),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SettingsSwitchRow(
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
internal fun SettingsChoiceRow(
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
            // Opens a dialog to choose from; TalkBack reads the label, description and value as one.
            .clickable(role = Role.Button, onClick = onClick)
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
internal fun SettingsActionRow(
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
internal fun SettingsToggleRow(
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
internal fun MethodChoiceRow(
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
internal fun ThemeOptionRow(
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
