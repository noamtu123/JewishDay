package com.turel.jewishdaynext.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.turel.jewishdaynext.R
import com.turel.jewishdaynext.ui.components.InfoCard
import com.turel.jewishdaynext.ui.components.ScreenPaddingValues
import com.turel.jewishdaynext.ui.components.ScreenSurface
import com.turel.jewishdaynext.ui.components.readableWidth
import com.turel.jewishdaynext.ui.localizedString

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    ScreenSurface(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .readableWidth()
                .fillMaxSize(),
            contentPadding = ScreenPaddingValues,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AboutHeader()
            }
            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = localizedString(R.string.about_body, R.string.about_body_hebrew),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = localizedString(R.string.about_tagline, R.string.about_tagline_hebrew),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
