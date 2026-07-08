// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class HebcalDailyLearningClient @Inject constructor() {
    fun fetchWindow(
        startDate: LocalDate,
        endDate: LocalDate,
        inIsrael: Boolean,
    ): Map<LocalDate, List<HebcalLearningEntry>> {
        val connection = URL(hebcalUrl(startDate, endDate, inIsrael)).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = ConnectTimeoutMillis
        connection.readTimeout = ReadTimeoutMillis
        connection.setRequestProperty("Accept", "application/json")
        return try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Hebcal returned HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader().use { reader -> reader.readText() }
            parseLearningEntries(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun hebcalUrl(startDate: LocalDate, endDate: LocalDate, inIsrael: Boolean): String {
        val params = listOf(
            "cfg" to "json",
            "v" to "1",
            "start" to startDate.toString(),
            "end" to endDate.toString(),
            "i" to if (inIsrael) "on" else "off",
            "F" to "on",
            "yyomi" to "on",
            "myomi" to "on",
            "dps" to "on",
            "dr1" to "on",
            "dr3" to "on",
            "dty" to "on",
            "dshl" to "on",
            "dksa" to "on",
        ).joinToString("&") { (key, value) -> "${key.urlEncode()}=${value.urlEncode()}" }
        return "https://www.hebcal.com/hebcal?$params"
    }

    private fun parseLearningEntries(body: String): Map<LocalDate, List<HebcalLearningEntry>> {
        val items = JSONObject(body).optJSONArray("items") ?: return emptyMap()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val category = item.optionalString("category") ?: continue
                if (category !in SupportedCategories) continue
                val date = item.optionalString("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: continue
                val title = item.optionalString("title") ?: continue
                add(
                    date to HebcalLearningEntry(
                        category = category,
                        title = title,
                        hebrew = item.optionalString("hebrew"),
                        memo = item.optionalString("memo"),
                    ),
                )
            }
        }.groupBy(
            keySelector = { (date, _) -> date },
            valueTransform = { (_, entry) -> entry },
        )
    }

    private companion object {
        const val ConnectTimeoutMillis = 5_000
        const val ReadTimeoutMillis = 10_000
        val SupportedCategories = setOf(
            "dafyomi",
            "mishnayomi",
            "tanakhYomi",
            "dailyPsalms",
            "dailyRambam1",
            "dailyRambam3",
            "yerushalmi",
            "shemiratHaLashon",
            "kitzurShulchanAruch",
        )
    }
}

data class HebcalLearningEntry(
    val category: String,
    val title: String,
    val hebrew: String? = null,
    val memo: String? = null,
)

private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

private fun JSONObject.optionalString(name: String): String? =
    if (has(name) && !isNull(name)) optString(name).takeIf(String::isNotBlank) else null