// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import com.noamtu.jewishday.R
import com.noamtu.jewishday.model.Festival
import com.noamtu.jewishday.ui.components.iconRes
import com.noamtu.jewishday.ui.theme.SkyFrame
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The widget and its picker previews, actually laid out and drawn: every text of every size must
 * sit whole inside its frame — no line cut off by a line limit, ellipsized or pushed below the
 * bottom edge — in English and in Hebrew. [dayWidgetLayoutFor] only estimates text; this is where
 * the estimate meets real fonts. Each render is also written to build/widget-renders to be looked at.
 *
 * The picker's previews before Android 12 are pictures rather than layouts, and those pictures are
 * these renders: preview-<size>-en.png and -iw.png, copied into res/drawable-nodpi and
 * drawable-iw-nodpi as day_widget_<size>_preview_image.png. A change to a preview layout is copied
 * over with it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class DayWidgetRenderTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun everyPreviewFitsItsSize() = assertPreviewsFit("en")

    @Test
    @Config(qualifiers = "+iw-ldrtl")
    fun everyPreviewFitsItsSizeInHebrew() = assertPreviewsFit("iw")

    @Test
    fun theWidgetFitsEverySize() = assertWidgetFits(englishDay, "en")

    @Test
    fun theWidgetFitsEverySizeOnAnOrdinaryDay() = assertWidgetFits(englishDay.copy(festival = null), "en-plain")

    /** Hebrew texts on a left-to-right device, where the widget mirrors itself by hand. */
    @Test
    fun theWidgetFitsEverySizeInHebrewOnAnEnglishDevice() = assertWidgetFits(hebrewDay, "he-on-en")

    @Test
    @Config(qualifiers = "+iw-ldrtl")
    fun theWidgetFitsEverySizeInHebrew() = assertWidgetFits(hebrewDay, "iw")

    @Test
    @Config(qualifiers = "+iw-ldrtl")
    fun theWidgetFitsEverySizeInHebrewOnAnOrdinaryDay() = assertWidgetFits(hebrewDay.copy(festival = null), "iw-plain")

    /** Every festival has a picture that draws, all of them written side by side to festivals.png. */
    @Test
    fun everyFestivalHasAPicture() {
        val cell = 144
        val sheet = Bitmap.createBitmap(cell * Festival.entries.size, cell, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet).apply { drawColor(0xFFAEC9F6.toInt()) }
        val blank = Festival.entries.filterIndexed { index, festival ->
            val picture = Bitmap.createBitmap(cell, cell, Bitmap.Config.ARGB_8888)
            requireNotNull(context.getDrawable(festival.iconRes)).apply { setBounds(0, 0, cell, cell) }.draw(Canvas(picture))
            canvas.drawBitmap(picture, (index * cell).toFloat(), 0f, null)
            // The pale disc alone paints pixels too, so a fifth of the disc must be the drawing's own.
            val disc = picture.getPixel(cell / 2, 2)
            val samples = (0 until cell step 4).flatMap { x -> (0 until cell step 4).map { y -> picture.getPixel(x, y) } }
                .filter { AndroidColor.alpha(it) > 0 }
            samples.count { it != disc } < samples.size / 5
        }
        File(File("build/widget-renders").apply { mkdirs() }, "festivals.png").outputStream()
            .use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("No picture drawn for $blank", blank.isEmpty())
    }

    private fun assertPreviewsFit(locale: String) {
        val problems = previews.flatMap { (name, layout, size) ->
            val view = laidOut(inflate(layout), size)
            save(view, "preview-$name-$locale")
            overflowIn(view).map { "$name preview at $size: $it" }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private fun assertWidgetFits(state: DayWidgetState, locale: String) {
        val problems = widgetSizes.flatMap { (name, size) ->
            val remoteViews = runBlocking { SampleWidget(state).compose(context, size = size) }
            val view = laidOut(remoteViews.apply(context, FrameLayout(context)), size)
            save(view, "widget-$name-$locale")
            overflowIn(view).map { "$name widget at $size: $it" }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private fun inflate(layout: Int): View = LayoutInflater.from(context).inflate(layout, FrameLayout(context), false)

    /**
     * [view] laid out at [size] inside a host frame that reads in the device's direction, as the
     * launcher's does: a view with no parent never resolves right-to-left.
     */
    private fun laidOut(view: View, size: DpSize): View {
        val density = context.resources.displayMetrics.density
        val width = (size.width.value * density).roundToInt()
        val height = (size.height.value * density).roundToInt()
        val host = FrameLayout(context).apply { layoutDirection = context.resources.configuration.layoutDirection }
        host.addView(view, FrameLayout.LayoutParams(width, height))
        host.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        host.layout(0, 0, width, height)
        return host
    }

    /** Every shown text that does not sit whole inside [root]: cut by its line limit, ellipsized or below the edge. */
    private fun overflowIn(root: View): List<String> = textViewsIn(root).filter { it.text.isNotEmpty() }
        .mapNotNull { text ->
            val box = Rect().also { text.getDrawingRect(it) }
            (root as ViewGroup).offsetDescendantRectToMyCoords(text, box)
            val layout = text.layout ?: return@mapNotNull "\"${text.text}\" was never laid out"
            val ellipsized = (0 until layout.lineCount).any { layout.getEllipsisCount(it) > 0 }
            when {
                layout.lineCount > text.maxLines -> "\"${text.text}\" needs ${layout.lineCount} lines, has ${text.maxLines}"
                ellipsized -> "\"${text.text}\" is ellipsized"
                box.bottom > root.height || text.height < layout.height -> "\"${text.text}\" runs past the bottom"
                else -> null
            }
        }

    /** The visible texts under [view]; isShown will not do, as it is false for any view off a window. */
    private fun textViewsIn(view: View): List<TextView> = when {
        view.visibility != View.VISIBLE -> emptyList()
        view is TextView -> listOf(view)
        view is ViewGroup -> (0 until view.childCount).flatMap { textViewsIn(view.getChildAt(it)) }
        else -> emptyList()
    }

    private fun save(view: View, name: String) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/widget-renders").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** The widget with a fixed day, so it can be composed without loading one. */
    private class SampleWidget(private val state: DayWidgetState) : GlanceAppWidget() {
        override val sizeMode: SizeMode = SizeMode.Exact

        override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent { DayWidgetContent(state) }
    }

    private companion object {
        // Each size as a phone launcher gives it (four columns 358 dp wide, two rows 202 dp tall),
        // and a tighter grid's, which the picker may draw the preview at.
        val previews = listOf(
            Triple("small", R.layout.day_widget_small_preview, DpSize(170.dp, 202.dp)),
            Triple("small-tight", R.layout.day_widget_small_preview, DpSize(130.dp, 150.dp)),
            Triple("medium", R.layout.day_widget_medium_preview, DpSize(358.dp, 202.dp)),
            Triple("medium-tight", R.layout.day_widget_medium_preview, DpSize(276.dp, 176.dp)),
            Triple("large", R.layout.day_widget_large_preview, DpSize(358.dp, 404.dp)),
            Triple("large-tight", R.layout.day_widget_large_preview, DpSize(276.dp, 350.dp)),
        )

        val widgetSizes = listOf(
            "strip" to DpSize(250.dp, 60.dp),
            "small" to DpSize(170.dp, 202.dp),
            "small-tight" to DpSize(130.dp, 150.dp),
            "medium" to DpSize(358.dp, 202.dp),
            "medium-tight" to DpSize(276.dp, 176.dp),
            "large" to DpSize(358.dp, 404.dp),
            "large-tight" to DpSize(276.dp, 350.dp),
        )

        val daySky = SkyFrame(
            base = Color(0xFFD6E4FA),
            blooms = listOf(Color(0xFF8DB3F2), Color(0xFFBFE0F2), Color(0xFFEEF4FF)),
            bloomAlpha = 0.75f,
            stars = 0f,
            sun = 1f,
            moon = 0f,
            sunArc = 0.7f,
            dark = false,
        )

        val englishDay = DayWidgetState(
            useHebrew = false,
            sky = daySky,
            hebrewDate = "14 Tishrei 5787",
            weekday = "Friday",
            festival = Festival.Sukkot,
            chip = "Sukkot",
            observanceLines = listOf("Yom Tov starts 18:02", "Yom Tov ends 19:01"),
            eventLine = null,
            times = listOf(
                DayWidgetTime("Mincha Ketana", "16:01"),
                DayWidgetTime("Plag Hamincha", "17:17"),
                DayWidgetTime("Candle Lighting", "18:02", pinned = true),
                DayWidgetTime("Sunset", "18:22"),
            ),
            learning = "Daf Yomi Bavli: Bechorot 7",
            locationName = "Times based on Jerusalem",
        )

        val hebrewDay = englishDay.copy(
            useHebrew = true,
            hebrewDate = "י״ד תשרי תשפ״ז",
            weekday = "יום שישי",
            chip = "סוכות",
            observanceLines = listOf("כניסת החג 18:02", "צאת החג 19:01"),
            times = listOf(
                DayWidgetTime("מנחה קטנה", "16:01"),
                DayWidgetTime("פלג המנחה", "17:17"),
                DayWidgetTime("הדלקת נרות", "18:02", pinned = true),
                DayWidgetTime("שקיעה", "18:22"),
            ),
            learning = "דף יומי בבלי: בכורות ז׳",
            locationName = "הזמנים מבוססים על ירושלים",
        )
    }
}
