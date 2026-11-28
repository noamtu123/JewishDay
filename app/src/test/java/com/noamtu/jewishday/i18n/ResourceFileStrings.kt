// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.i18n

import com.noamtu.jewishday.R
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * A [StringSource] read straight from the app's resource files, for unit tests, where Android's
 * `Resources` do not exist. Strings are unescaped the way aapt does it and formatted the way
 * `Resources.getString(id, args)` does, so a test sees exactly the text a phone would.
 *
 * [folders] are searched in order — a translation first, then `values` — just as Android falls back.
 */
class ResourceFileStrings(private val folders: List<String>, private val locale: Locale) : StringSource {
    private val strings = HashMap<String, String>()
    private val plurals = HashMap<String, Map<String, String>>()

    init {
        // Later folders only fill in what earlier ones lack.
        for (folder in folders.reversed()) {
            val dir = File(ResDir, folder)
            dir.listFiles { file -> file.extension == "xml" }.orEmpty().sorted().forEach(::load)
        }
    }

    private fun load(file: File) {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = document.documentElement
        val stringNodes = root.getElementsByTagName("string")
        for (index in 0 until stringNodes.length) {
            val element = stringNodes.item(index) as Element
            strings[element.getAttribute("name")] = unescape(element.textContent)
        }
        val pluralNodes = root.getElementsByTagName("plurals")
        for (index in 0 until pluralNodes.length) {
            val element = pluralNodes.item(index) as Element
            val items = element.getElementsByTagName("item")
            plurals[element.getAttribute("name")] = (0 until items.length)
                .map { items.item(it) as Element }
                .associate { it.getAttribute("quantity") to unescape(it.textContent) }
        }
    }

    override fun getString(id: Int): String = strings[name(R.string::class.java, id)]
        ?: error("No string resource ${name(R.string::class.java, id)} in $folders")

    override fun getString(id: Int, vararg args: Any): String = String.format(locale, getString(id), *args)

    override fun getQuantityString(id: Int, count: Int, vararg args: Any): String {
        val name = name(R.plurals::class.java, id)
        val forms = plurals[name] ?: error("No plurals resource $name in $folders")
        val text = forms[quantity(count)] ?: forms.getValue("other")
        return String.format(locale, text, *args)
    }

    /** CLDR's plural categories, for the languages the app has. Android falls back to "other". */
    private fun quantity(count: Int): String = when (locale.language) {
        "en" -> if (count == 1) "one" else "other"
        "he", "iw" -> when (count) {
            1 -> "one"
            2 -> "two"
            else -> "other"
        }
        else -> "other"
    }

    companion object {
        private val ResDir = File("src/main/res")
        private val names = HashMap<Pair<Class<*>, Int>, String>()

        private fun name(type: Class<*>, id: Int): String = names.getOrPut(type to id) {
            type.fields.firstOrNull { it.getInt(null) == id }?.name ?: error("No ${type.simpleName} resource with id $id")
        }

        /** aapt's rules: backslash escapes, quotes that keep whitespace, and runs of spaces collapsed. */
        fun unescape(raw: String): String {
            val out = StringBuilder()
            var quoted = false
            var index = 0
            var pendingSpace = false
            while (index < raw.length) {
                val char = raw[index]
                when {
                    char == '\\' && index + 1 < raw.length -> {
                        if (pendingSpace) out.append(' ').also { pendingSpace = false }
                        val next = raw[index + 1]
                        when (next) {
                            'n' -> out.append('\n')
                            't' -> out.append('\t')
                            'u' -> {
                                out.append(raw.substring(index + 2, index + 6).toInt(16).toChar())
                                index += 4
                            }
                            else -> out.append(next)
                        }
                        index += 2
                        continue
                    }
                    char == '"' -> quoted = !quoted
                    !quoted && char.isWhitespace() -> pendingSpace = out.isNotEmpty()
                    else -> {
                        if (pendingSpace) out.append(' ').also { pendingSpace = false }
                        out.append(char)
                    }
                }
                index++
            }
            return out.toString()
        }
    }
}

/** Resolvers over the real resource files, one per language the app has. */
object TestText {
    val english: TextResolver by lazy { resolver("en") }
    val hebrew: TextResolver by lazy { resolver("he") }

    fun resolver(tag: String): TextResolver {
        val folders = when (tag) {
            "en" -> listOf("values")
            "he" -> listOf("values-iw", "values")
            else -> listOf("values-$tag", "values")
        }
        val locale = Locale.forLanguageTag(tag)
        return TextResolver(ResourceFileStrings(folders, locale), locale, isRtl = tag == "he")
    }
}

/** This text as the English app shows it. */
fun UiText.en(): String = TestText.english.resolve(this)

/** This text as the Hebrew app shows it. */
fun UiText.he(): String = TestText.hebrew.resolve(this)
