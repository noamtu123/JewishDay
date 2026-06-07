package com.turel.jewishdaynext.data

import android.content.Context
import com.turel.jewishdaynext.model.ZmanItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject

interface DailyLearningRepository {
    fun learningItems(
        date: LocalDate,
        inIsrael: Boolean,
        includeRambamThreeChapters: Boolean,
    ): Flow<List<ZmanItem>>
}

@Singleton
class HebcalDailyLearningRepository @Inject constructor(
    private val cache: DailyLearningCache,
    private val client: HebcalDailyLearningClient,
    private val clock: Clock,
) : DailyLearningRepository {
    override fun learningItems(
        date: LocalDate,
        inIsrael: Boolean,
        includeRambamThreeChapters: Boolean,
    ): Flow<List<ZmanItem>> = flow {
        val cached = cache.read(date, inIsrael)
        if (cached.isNotEmpty()) {
            emit(cached.toZmanItems(includeRambamThreeChapters))
        }

        val now = clock.instant()
        if (cache.shouldRefresh(date = date, inIsrael = inIsrael, now = now)) {
            val endDate = date.plusDays(CacheWindowDays - 1)
            runCatching {
                client.fetchWindow(startDate = date, endDate = endDate, inIsrael = inIsrael)
            }.onSuccess { days ->
                cache.writeWindow(
                    startDate = date,
                    endDate = endDate,
                    inIsrael = inIsrael,
                    refreshedAt = now,
                    days = days,
                )
                val updated = cache.read(date, inIsrael)
                if (updated.isNotEmpty()) {
                    emit(updated.toZmanItems(includeRambamThreeChapters))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val CacheWindowDays = 7L
    }
}

interface DailyLearningCache {
    fun read(date: LocalDate, inIsrael: Boolean): List<HebcalLearningEntry>
    fun shouldRefresh(date: LocalDate, inIsrael: Boolean, now: Instant): Boolean
    fun writeWindow(
        startDate: LocalDate,
        endDate: LocalDate,
        inIsrael: Boolean,
        refreshedAt: Instant,
        days: Map<LocalDate, List<HebcalLearningEntry>>,
    )
}

@Singleton
class SharedPreferencesDailyLearningCache @Inject constructor(
    @ApplicationContext context: Context,
) : DailyLearningCache {
    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    override fun read(date: LocalDate, inIsrael: Boolean): List<HebcalLearningEntry> =
        preferences.getString(dayKey(date, inIsrael), null)
            ?.let(DailyLearningCacheCodec::decode)
            ?: emptyList()

    override fun shouldRefresh(date: LocalDate, inIsrael: Boolean, now: Instant): Boolean {
        if (read(date, inIsrael).isEmpty()) return true
        val windowEnd = preferences.getString(windowEndKey(inIsrael), null)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val refreshedAt = preferences.getLong(refreshedAtKey(inIsrael), 0L)
        return windowEnd == null ||
            date.plusDays(CacheWindowDays - 1).isAfter(windowEnd) ||
            refreshedAt <= 0L ||
            now.toEpochMilli() - refreshedAt > CacheMaxAge.toMillis()
    }

    override fun writeWindow(
        startDate: LocalDate,
        endDate: LocalDate,
        inIsrael: Boolean,
        refreshedAt: Instant,
        days: Map<LocalDate, List<HebcalLearningEntry>>,
    ) {
        preferences.edit().apply {
            putString(windowStartKey(inIsrael), startDate.toString())
            putString(windowEndKey(inIsrael), endDate.toString())
            putLong(refreshedAtKey(inIsrael), refreshedAt.toEpochMilli())
            days.forEach { (date, entries) ->
                putString(dayKey(date, inIsrael), DailyLearningCacheCodec.encode(entries))
            }
        }.apply()
    }

    private fun dayKey(date: LocalDate, inIsrael: Boolean): String = "${scope(inIsrael)}:day:$date"
    private fun windowStartKey(inIsrael: Boolean): String = "${scope(inIsrael)}:window_start"
    private fun windowEndKey(inIsrael: Boolean): String = "${scope(inIsrael)}:window_end"
    private fun refreshedAtKey(inIsrael: Boolean): String = "${scope(inIsrael)}:refreshed_at"
    private fun scope(inIsrael: Boolean): String = if (inIsrael) "israel" else "diaspora"

    private companion object {
        const val PreferencesName = "daily_learning_cache"
        const val CacheWindowDays = 7L
        val CacheMaxAge: Duration = Duration.ofDays(7)
    }
}

@Singleton
class HebcalDailyLearningClient @Inject constructor() {
    fun fetchWindow(
        startDate: LocalDate,
        endDate: LocalDate,
        inIsrael: Boolean,
    ): Map<LocalDate, List<HebcalLearningEntry>> {
        val connection = URL(hebcalUrl(startDate, endDate, inIsrael)).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = TimeoutMillis
        connection.readTimeout = TimeoutMillis
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
        const val TimeoutMillis = 5_000
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

internal object DailyLearningCacheCodec {
    fun encode(entries: List<HebcalLearningEntry>): String = entries.joinToString("\n") { entry ->
        listOf(
            entry.category.encodeField(),
            entry.title.encodeField(),
            entry.hebrew.encodeField(),
            entry.memo.encodeField(),
        ).joinToString("\t")
    }

    fun decode(value: String): List<HebcalLearningEntry> = value.lineSequence()
        .mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size != 4) return@mapNotNull null
            val category = parts[0].decodeField() ?: return@mapNotNull null
            val title = parts[1].decodeField() ?: return@mapNotNull null
            HebcalLearningEntry(
                category = category,
                title = title,
                hebrew = parts[2].decodeField(),
                memo = parts[3].decodeField(),
            )
        }
        .toList()

    private fun String?.encodeField(): String = this
        ?.let { Base64.getEncoder().encodeToString(it.toByteArray(Charsets.UTF_8)) }
        ?: ""

    private fun String.decodeField(): String? = takeIf(String::isNotEmpty)
        ?.let { String(Base64.getDecoder().decode(it), Charsets.UTF_8) }
}

internal fun List<HebcalLearningEntry>.toZmanItems(includeRambamThreeChapters: Boolean): List<ZmanItem> {
    val byCategory = groupBy(HebcalLearningEntry::category)
    fun entry(category: String): HebcalLearningEntry? = byCategory[category]?.firstOrNull()

    return buildList {
        entry("dafyomi")?.let {
            add(it.toZmanItem("Daf Yomi Bavli", "דף יומי בבלי", "Hebcal Daf Yomi cycle", "מחזור דף יומי של Hebcal"))
        }
        entry("mishnayomi")?.let {
            add(it.toZmanItem("Mishnah Yomi", "משנה יומית", "Hebcal Mishnah Yomi cycle", "מחזור משנה יומית של Hebcal"))
        }
        entry("yerushalmi")?.let {
            add(it.toZmanItem("Daf Yomi Yerushalmi", "דף יומי ירושלמי", "Hebcal Yerushalmi cycle", "מחזור ירושלמי של Hebcal"))
        }
        entry("dailyPsalms")?.let {
            add(it.toZmanItem("Tehillim Yomi", "תהילים יומי", "Daily Tehillim division", "חלוקת תהילים יומית"))
        }

        val rambamOne = entry("dailyRambam1")
        val rambamThree = entry("dailyRambam3")?.takeIf { includeRambamThreeChapters }
        if (rambamOne != null || rambamThree != null) {
            add(rambamItem(rambamOne, rambamThree))
        }

        entry("shemiratHaLashon")?.let {
            add(it.toZmanItem("Shemirat HaLashon", "שמירת הלשון", "Hebcal daily Shemirat HaLashon", "שמירת הלשון יומית של Hebcal"))
        }
        entry("kitzurShulchanAruch")?.let {
            add(it.toZmanItem("Kitzur Shulchan Aruch", "קיצור שולחן ערוך", "Daily halacha from Hebcal", "הלכה יומית של Hebcal"))
        }
        entry("tanakhYomi")?.let {
            add(it.toZmanItem("Tanakh Yomi", "תנ״ך יומי", "Hebcal Tanakh Yomi cycle", "מחזור תנ״ך יומי של Hebcal"))
        }
    }
}

private fun HebcalLearningEntry.toZmanItem(
    title: String,
    titleHebrew: String,
    description: String,
    descriptionHebrew: String,
): ZmanItem = ZmanItem(
    title = title,
    titleHebrew = titleHebrew,
    time = null,
    description = description,
    descriptionHebrew = descriptionHebrew,
    value = displayEnglish(),
    valueHebrew = displayHebrew(),
)

private fun rambamItem(
    rambamOne: HebcalLearningEntry?,
    rambamThree: HebcalLearningEntry?,
): ZmanItem {
    val oneEnglish = rambamOne?.displayEnglish()
    val oneHebrew = rambamOne?.displayHebrew()
    val threeEnglish = rambamThree?.displayEnglish()
    val threeHebrew = rambamThree?.displayHebrew()
    return ZmanItem(
        title = "Rambam Yomi",
        titleHebrew = "רמב״ם יומי",
        time = null,
        description = if (rambamThree == null) "Hebcal Rambam, 1 chapter" else "Hebcal Rambam, 1 and 3 chapter tracks",
        descriptionHebrew = if (rambamThree == null) "רמב״ם יומי של Hebcal, פרק אחד" else "רמב״ם יומי של Hebcal, פרק אחד ושלושה פרקים",
        value = listOfNotNull(
            oneEnglish?.let { "1 chapter: $it" },
            threeEnglish?.let { "3 chapters: $it" },
        ).joinToString(" · "),
        valueHebrew = listOfNotNull(
            oneHebrew?.let { "פרק אחד: $it" },
            threeHebrew?.let { "3 פרקים: $it" },
        ).joinToString(" · "),
    )
}

private fun HebcalLearningEntry.displayEnglish(): String = memo?.takeIf(String::isNotBlank) ?: title
private fun HebcalLearningEntry.displayHebrew(): String = hebrew?.takeIf(String::isNotBlank) ?: displayEnglish()

private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

private fun JSONObject.optionalString(name: String): String? =
    if (has(name) && !isNull(name)) optString(name).takeIf(String::isNotBlank) else null
