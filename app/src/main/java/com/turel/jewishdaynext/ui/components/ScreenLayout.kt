package com.turel.jewishdaynext.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val ScreenHorizontalPadding = 24.dp
val ScreenVerticalPadding = 24.dp
val ScreenContentMaxWidth = 760.dp

val ScreenPaddingValues = PaddingValues(
    horizontal = ScreenHorizontalPadding,
    vertical = ScreenVerticalPadding,
)

fun Modifier.readableWidth(maxWidth: Dp = ScreenContentMaxWidth): Modifier =
    widthIn(max = maxWidth).fillMaxWidth()

@Composable
fun ScreenSurface(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = contentAlignment,
        ) {
            content()
        }
    }
}
