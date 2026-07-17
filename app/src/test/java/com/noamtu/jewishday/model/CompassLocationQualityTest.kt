// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompassLocationQualityTest {
    @Test
    fun freshAccurateFixCanAuthorizeAlignment() {
        assertTrue(isCompassLocationTrusted(true, 50f, 10_000L, 10_000.0))
    }

    @Test
    fun staleOrUnknownFixCannotAuthorizeAlignment() {
        assertFalse(isCompassLocationTrusted(true, 20f, CompassLocationMaxAgeMillis + 1L, 10_000.0))
        assertFalse(isCompassLocationTrusted(true, null, 0L, 10_000.0))
        assertFalse(isCompassLocationTrusted(true, Float.NaN, 0L, 10_000.0))
        assertFalse(isCompassLocationTrusted(false, 20f, 0L, 10_000.0))
    }

    @Test
    fun accuracyRequirementTightensNearTheTarget() {
        assertTrue(isCompassLocationTrusted(true, 5_000f, 0L, 9_000_000.0))
        assertFalse(isCompassLocationTrusted(true, 5_000f, 0L, 2_000.0))
        assertFalse(isCompassLocationTrusted(true, 1f, 0L, 0.0))
    }
}
