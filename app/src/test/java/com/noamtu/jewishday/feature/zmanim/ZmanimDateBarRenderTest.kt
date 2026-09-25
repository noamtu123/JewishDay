// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.zmanim

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.model.Festival
import com.noamtu.jewishday.ui.theme.JewishDayTheme
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Draws the zmanim screen's date card on a festival and on an ordinary day, in English and in Hebrew,
 * to `build/app-renders` for a look, and checks the festival's picture is actually drawn.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class ZmanimDateBarRenderTest {
    private val sukkot = ZmanimHeaderUi(
        jewishDate = "14 Tishrei 5787",
        jewishDateHebrew = "י״ד תשרי תשפ״ז",
        gregorianDate = "Friday, September 25",
        gregorianDateHebrew = "יום שישי, 25 בספטמבר",
        weekday = "Friday",
        weekdayHebrew = "יום שישי",
        festival = Festival.Sukkot,
        dayName = "Erev Sukkot",
        dayNameHebrew = "ערב סוכות",
    )

    @Test
    fun theFestivalsPictureIsDrawnBesideTheDate() = assertPictureDrawn(useHebrew = false, "en")

    @Test
    @Config(qualifiers = "+iw-ldrtl")
    fun theFestivalsPictureIsDrawnBesideTheDateInHebrew() = assertPictureDrawn(useHebrew = true, "iw")

    private fun assertPictureDrawn(useHebrew: Boolean, tag: String) {
        val festival = render(sukkot, useHebrew, "datebar-$tag")
        val plain = render(sukkot.copy(festival = null), useHebrew, "datebar-$tag-plain")

        assertFalse(festival.sameAs(plain))
    }

    private fun render(header: ZmanimHeaderUi, useHebrew: Boolean, name: String): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val direction = if (useHebrew) LayoutDirection.Rtl else LayoutDirection.Ltr
        activity.setContent {
            JewishDayTheme {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    DateBar(header = header, useHebrew = useHebrew, modifier = Modifier.fillMaxWidth().padding(16.dp))
                }
            }
        }
        shadowOf(Looper.getMainLooper()).idle()
        val root = activity.findViewById<View>(android.R.id.content)
        root.measure(
            View.MeasureSpec.makeMeasureSpec(activity.resources.displayMetrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        val bitmap = Bitmap.createBitmap(root.measuredWidth, root.measuredHeight, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        File("build/app-renders").apply { mkdirs() }.resolve("$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return bitmap
    }
}
