// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.ui.theme.LocalGlassStyle
import com.noamtu.jewishday.ui.theme.LocalGlassTheme

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    elevation: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Glass: a frosted pane with a lit edge instead of a raised card. A shadow under a
    // see-through card shows through it as a dark smudge, so there is none. A card given its own
    // colour (a highlight) keeps it, faded to the same glassiness so it still stands out.
    if (LocalGlassTheme.current) {
        val isDefault = containerColor == MaterialTheme.colorScheme.surfaceVariant
        val glass = LocalGlassStyle.current
        OutlinedCard(
            modifier = modifier,
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (isDefault) glass.fill else containerColor.copy(alpha = 0.35f),
            ),
            border = BorderStroke(1.dp, glass.edge),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content,
            )
        }
        return
    }
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content,
        )
    }
}
