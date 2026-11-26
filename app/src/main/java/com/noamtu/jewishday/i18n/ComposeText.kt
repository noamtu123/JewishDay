// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.i18n

import android.content.res.Configuration
import android.os.LocaleList
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.LayoutDirection

/** The resolver for the language this part of the screen is in. Provided by [ProvideTextResolver]. */
val LocalTextResolver = staticCompositionLocalOf<TextResolver> {
    error("No TextResolver: wrap the content in ProvideTextResolver")
}

/** Words this text in the current language. */
@Composable
@ReadOnlyComposable
fun UiText.resolve(): String = LocalTextResolver.current.resolve(this)

/**
 * Makes [LocalTextResolver] follow the activity's resources, which are already in the app's language
 * (Android applies the per-app language itself on 13+, and [AppLocales.wrap] does on older versions).
 */
@Composable
fun ProvideTextResolver(content: @Composable () -> Unit) {
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current
    val resolver = remember(resources, configuration) {
        TextResolver(
            strings = ResourcesStringSource(resources),
            locale = AppLocales.displayLocale(configuration),
            isRtl = configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL,
        )
    }
    CompositionLocalProvider(LocalTextResolver provides resolver, content = content)
}

/**
 * Shows [content] in another language than the app's — string resources, [UiText] and layout
 * direction alike. Only for a part of the screen that is about that language's text, such as release
 * notes shown in English on request.
 */
@Composable
fun ProvideLanguage(tag: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localized = remember(context, configuration, tag) {
        val override = Configuration(configuration).apply { setLocales(LocaleList.forLanguageTags(tag)) }
        context.createConfigurationContext(override).resources
    }
    val localizedConfiguration = localized.configuration
    val rtl = localizedConfiguration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    val resolver = remember(localized) {
        TextResolver(ResourcesStringSource(localized), AppLocales.displayLocale(localizedConfiguration), rtl)
    }
    CompositionLocalProvider(
        LocalResources provides localized,
        LocalConfiguration provides localizedConfiguration,
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalTextResolver provides resolver,
        content = content,
    )
}
