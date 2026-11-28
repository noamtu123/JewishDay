// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.i18n

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Comment
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * What a translator relies on, checked on every build: every English string says where it appears
 * and what it means, and every translation keeps the placeholders of the English it translates — a
 * dropped or renumbered `%1$s` crashes or garbles the screen it is on. (Lint checks that nothing is
 * left untranslated.)
 */
class TranslationResourcesTest {
    private val res = File("src/main/res")

    private data class Entry(val name: String, val element: Element, val comment: String?)

    private fun entries(file: File): List<Entry> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val result = mutableListOf<Entry>()
        var lastComment: String? = null
        val children = document.documentElement.childNodes
        for (index in 0 until children.length) {
            val node = children.item(index)
            when {
                node is Comment -> lastComment = node.data.trim()
                node is Element && (node.tagName == "string" || node.tagName == "plurals") -> {
                    result += Entry(node.getAttribute("name"), node, lastComment)
                    lastComment = null
                }
                node.nodeType == Node.TEXT_NODE && node.textContent.isBlank() -> Unit
                else -> lastComment = null
            }
        }
        return result
    }

    private val source by lazy { entries(File(res, "values/strings.xml")) }

    private val translations: Map<String, List<Entry>> by lazy {
        res.listFiles().orEmpty()
            .filter { it.name.startsWith("values-") && File(it, "strings.xml").isFile }
            .associate { it.name to entries(File(it, "strings.xml")) }
    }

    private fun placeholders(text: String): List<String> =
        Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[a-zA-Z%]""").findAll(text).map { it.value }.sorted().toList()

    private fun Element.text(quantity: String = "other"): String =
        if (tagName == "plurals") {
            val items = getElementsByTagName("item")
            (0 until items.length).map { items.item(it) as Element }.first { it.getAttribute("quantity") == quantity }.textContent
        } else {
            textContent
        }

    @Test
    fun everyEnglishStringTellsTheTranslatorWhereItAppears() {
        val uncommented = source.filter { it.comment.isNullOrBlank() }.map { it.name }
        assertTrue("Strings without a comment above them: $uncommented", uncommented.isEmpty())
    }

    @Test
    fun thereIsAtLeastOneTranslation() {
        assertTrue(translations.keys.toString(), "values-iw" in translations)
    }

    @Test
    fun everyTranslationKeepsThePlaceholdersOfItsEnglish() {
        val english = source.associateBy { it.name }
        for ((folder, entries) in translations) {
            for (entry in entries) {
                val original = english[entry.name]
                assertTrue("$folder has ${entry.name}, which English no longer has", original != null)
                original!!
                assertEquals("$folder/${entry.name} is a ${original.element.tagName} in English", original.element.tagName, entry.element.tagName)
                assertEquals(
                    "$folder/${entry.name} must keep the English placeholders",
                    placeholders(original.element.text()),
                    placeholders(entry.element.text()),
                )
                assertTrue(
                    "$folder/${entry.name} is marked untranslatable in English",
                    original.element.getAttribute("translatable") != "false",
                )
            }
        }
    }
}
