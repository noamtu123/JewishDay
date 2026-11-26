// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.i18n

import android.content.Context
import android.content.res.Resources
import android.view.View
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.noamtu.jewishday.R
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Where the words come from: Android's resources in the app, the resource files in unit tests. */
interface StringSource {
    fun getString(@StringRes id: Int): String
    fun getString(@StringRes id: Int, vararg args: Any): String
    fun getQuantityString(@PluralsRes id: Int, count: Int, vararg args: Any): String
}

/** [StringSource] over a [Resources] already configured for the language to show. */
class ResourcesStringSource(private val resources: Resources) : StringSource {
    override fun getString(id: Int): String = resources.getString(id)
    override fun getString(id: Int, vararg args: Any): String = resources.getString(id, *args)
    override fun getQuantityString(id: Int, count: Int, vararg args: Any): String =
        resources.getQuantityString(id, count, *args)
}

/**
 * Words [UiText] in one language: string resources from [strings], the calendar's names from that
 * language's [LanguageFormatter], dates and times through java.time in [locale].
 *
 * [locale] is the language of the translation actually showing (see [AppLocales.displayLocale]),
 * without a region: an English phone set to en-GB still reads "6:45 AM", exactly as the app always
 * has, and a phone in a language the app has no translation for gets English dates under English
 * text rather than its own.
 */
class TextResolver(
    private val strings: StringSource,
    val locale: Locale,
    /** Whether the language reads right to left — widgets lay themselves out by it. */
    val isRtl: Boolean,
) {
    val language: LanguageFormatter = LanguageFormatter.forLocale(locale)

    private val formatters = HashMap<String, DateTimeFormatter>()

    private fun formatter(pattern: String): DateTimeFormatter =
        formatters.getOrPut(pattern) { DateTimeFormatter.ofPattern(pattern, locale) }

    fun string(@StringRes id: Int, vararg args: Any): String = resolve(UiText.Res(id, args.toList()))

    fun resolve(text: UiText): String = when (text) {
        is UiText.Res ->
            if (text.args.isEmpty()) strings.getString(text.id) else strings.getString(text.id, *resolveArgs(text.args))
        is UiText.Plural -> strings.getQuantityString(text.id, text.count, *resolveArgs(text.args))
        is UiText.Verbatim -> text.text
        is UiText.Calendar -> language.format(text.text)
        is UiText.Time -> {
            val clock = if (text.use24Hour) "HH:mm" else "h:mm a"
            val pattern = if (text.withWeekday) "EEE $clock" else clock
            formatter(pattern).withZone(text.zoneId).format(text.instant)
        }
        is UiText.Date -> when (text.style) {
            DateStyle.Weekday -> text.date.format(formatter("EEEE"))
            DateStyle.ShortWeekday -> text.date.format(formatter("EEE"))
            DateStyle.DayMonth -> text.date.format(formatter(strings.getString(R.string.date_pattern_day_month)))
        }
    }

    private fun resolveArgs(args: List<Any>): Array<Any> =
        args.map { if (it is UiText) resolve(it) else it }.toTypedArray()

    companion object {
        /**
         * The resolver for the app's language, for code outside a composition — widgets, the
         * notification. The context may be the application's: [AppLocales.localizedContext] gives it
         * the app's own language first, which Android 12 and older do not do by themselves.
         */
        fun of(context: Context): TextResolver {
            val resources = AppLocales.localizedContext(context).resources
            return TextResolver(
                strings = ResourcesStringSource(resources),
                locale = AppLocales.displayLocale(resources.configuration),
                isRtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL,
            )
        }
    }
}
