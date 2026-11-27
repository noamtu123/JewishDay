// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.notification

import com.noamtu.jewishday.core.model.JewishLocation
import com.noamtu.jewishday.core.model.TzeitHakochavimMethod
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.defaultJerusalemLocation
import java.io.File
import java.time.ZoneId
import java.util.Collections
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** What makes the status-bar date icon work itself out again, besides its own alarm. */
class DateStatusIconTriggersTest {
    @Test
    fun aNewFixOrOpinionRedrawsTheIconButTheSameOnesDoNot() = runBlocking {
        val settings = MutableStateFlow(ZmanimCalculationSettings())
        val location = MutableStateFlow<JewishLocation?>(null)
        val changes = Collections.synchronizedList(mutableListOf<Unit>())
        val watcher = launch(Dispatchers.Unconfined) { statusIconInputChanges(settings, location).collect { changes += it } }

        // What the service has just rendered from is not a change.
        assertEquals(0, changes.size)
        // The first fix after rendering Jerusalem.
        location.value = defaultJerusalemLocation.copy(name = "Here", latitude = 40.7128, longitude = -74.006, zoneId = ZoneId.of("America/New_York"))
        assertEquals(1, changes.size)
        // A time-zone change re-stamps the same fix with the new zone.
        location.value = location.value?.copy(zoneId = ZoneId.of("Europe/London"))
        assertEquals(2, changes.size)
        // Another tzeit opinion moves the moment the Hebrew date rolls.
        val other = TzeitHakochavimMethod.entries.first { it != settings.value.tzeitHakochavimMethod }
        settings.value = settings.value.copy(tzeitHakochavimMethod = other)
        assertEquals(3, changes.size)
        // The same values again change nothing.
        settings.value = settings.value.copy()
        yield()
        assertEquals(3, changes.size)
        watcher.cancelAndJoin()
    }

    @Test
    fun aTimeZoneOrClockChangeReachesTheIcon() {
        val manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
        val receivers = manifest.getElementsByTagName("receiver")
        val iconReceiver = (0 until receivers.length).map { receivers.item(it) as Element }
            .single { it.getAttribute("android:name") == ".notification.DateStatusIconBootReceiver" }
        val actionNodes = iconReceiver.getElementsByTagName("action")
        val actions = (0 until actionNodes.length).map { (actionNodes.item(it) as Element).getAttribute("android:name") }
        // Without them a flight left the icon's weekday turning at the old zone's midnight until the
        // next alarm, set for the old zone too.
        assertTrue(actions.toString(), "android.intent.action.TIMEZONE_CHANGED" in actions)
        assertTrue(actions.toString(), "android.intent.action.TIME_SET" in actions)
    }
}
