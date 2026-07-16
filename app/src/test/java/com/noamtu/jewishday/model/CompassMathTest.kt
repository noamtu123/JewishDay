// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val StepNanos = 20_000_000L // 20 ms ≈ 50 Hz, the compass sampling period

class CompassMathTest {
    // --- angle helpers ---

    @Test
    fun normalizeDegreesWrapsIntoZeroTo360() {
        assertEquals(350f, normalizeDegrees(-10f), 1e-4f)
        assertEquals(10f, normalizeDegrees(370f), 1e-4f)
        assertEquals(0f, normalizeDegrees(720f), 1e-4f)
        assertEquals(180f, normalizeDegrees(-180f), 1e-4f)
        assertEquals(0f, normalizeDegrees(0f), 1e-4f)
    }

    @Test
    fun shortestSignedDeltaTakesTheShortWayAcrossNorth() {
        assertEquals(20f, shortestSignedDeltaDegrees(350f, 10f), 1e-4f)
        assertEquals(-20f, shortestSignedDeltaDegrees(10f, 350f), 1e-4f)
        assertEquals(180f, shortestSignedDeltaDegrees(0f, 180f), 1e-4f)
        assertEquals(-179f, shortestSignedDeltaDegrees(0f, 181f), 1e-4f)
        assertEquals(0f, shortestSignedDeltaDegrees(90f, 90f), 1e-4f)
    }

    @Test
    fun angularDistanceIsSymmetricAndBounded() {
        assertEquals(20f, angularDistanceDegrees(350f, 10f), 1e-4f)
        assertEquals(20f, angularDistanceDegrees(10f, 350f), 1e-4f)
        assertEquals(180f, angularDistanceDegrees(0f, 180f), 1e-4f)
    }

    @Test
    fun trueNorthCorrectionAppliesDeclinationAcrossTheBoundary() {
        assertEquals(5f, trueHeadingDegrees(350f, 15f), 1e-4f)
        assertEquals(355f, trueHeadingDegrees(10f, -15f), 1e-4f)
        assertEquals(90f, trueHeadingDegrees(85f, 5f), 1e-4f)
    }

    @Test
    fun targetDirectionOnScreenCombinesBearingAndHeading() {
        assertEquals(90f, targetDirectionOnScreen(90f, 0f), 1e-4f)
        assertEquals(0f, targetDirectionOnScreen(90f, 90f), 1e-4f)
        assertEquals(20f, targetDirectionOnScreen(10f, 350f), 1e-4f)
        assertEquals(340f, targetDirectionOnScreen(350f, 10f), 1e-4f)
        assertEquals(0f, targetDirectionOnScreen(123.4f, 123.4f), 1e-4f)
        assertEquals(180f, targetDirectionOnScreen(5f, 185f), 1e-4f)
    }

    // --- rotation matrix → screen-relative heading ---
    // Matrices follow the SensorManager convention: row-major device→world, world = (E, N, U),
    // so columns are the device X (right edge), Y (top edge), Z (out of the screen) axes.

    private val flatTopNorth = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    private val flatTopEast = floatArrayOf(0f, 1f, 0f, -1f, 0f, 0f, 0f, 0f, 1f)
    private val uprightFacingNorth = floatArrayOf(1f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f)
    private val uprightFacingEast = floatArrayOf(0f, 0f, -1f, -1f, 0f, 0f, 0f, 1f, 0f)

    @Test
    fun flatDeviceHeadingFollowsTopEdge() {
        assertEquals(0f, screenRelativeHeadingDegrees(flatTopNorth, 0)!!, 0.01f)
        assertEquals(90f, screenRelativeHeadingDegrees(flatTopEast, 0)!!, 0.01f)
    }

    @Test
    fun uprightDeviceHeadingFollowsBackCamera() {
        assertEquals(0f, screenRelativeHeadingDegrees(uprightFacingNorth, 0)!!, 0.01f)
        assertEquals(90f, screenRelativeHeadingDegrees(uprightFacingEast, 0)!!, 0.01f)
    }

    @Test
    fun tiltedDeviceBlendsSmoothlyWithoutChangingHeading() {
        // Tipped up 45° about the east axis, top edge toward north: both references agree.
        val c = 0.70710677f
        val tilted45 = floatArrayOf(1f, 0f, 0f, 0f, c, -c, 0f, c, c)
        assertEquals(0f, screenRelativeHeadingDegrees(tilted45, 0)!!, 0.01f)
    }

    /** Device pitched [degrees] up about the east axis, top edge starting toward north. */
    private fun pitchedAboutEastAxis(degrees: Int): FloatArray {
        val c = Math.cos(Math.toRadians(degrees.toDouble())).toFloat()
        val s = Math.sin(Math.toRadians(degrees.toDouble())).toFloat()
        return floatArrayOf(1f, 0f, 0f, 0f, c, -s, 0f, s, c)
    }

    @Test
    fun pitchSweepFlipsOnlyDeepIntoFaceDown() {
        // Tipping the phone from flat (0°) through upright (90°) all the way over to face-down
        // (180°): the reading must stay a clean north until deep into face-down, pass through a
        // narrow "unavailable" band at the singularity (~147°), then read south — where the top
        // edge genuinely points. No other value may ever appear during a pure pitch.
        for (degrees in 0..144) {
            val heading = screenRelativeHeadingDegrees(pitchedAboutEastAxis(degrees), 0)
            assertEquals("pitch $degrees°", 0f, heading!!, 0.01f)
        }
        for (degrees in 145..149) {
            assertNull("pitch $degrees° should be ambiguous", screenRelativeHeadingDegrees(pitchedAboutEastAxis(degrees), 0))
        }
        for (degrees in 150..180) {
            val heading = screenRelativeHeadingDegrees(pitchedAboutEastAxis(degrees), 0)
            assertEquals("pitch $degrees°", 180f, heading!!, 0.01f)
        }
    }

    @Test
    fun recoverThresholdWidensTheAmbiguousBandForHysteresis() {
        // After a null, callers demand the higher recover strength: attitudes that would read
        // (noisily) right beside the singularity stay unavailable until the device moves clearly
        // past it, so noise cannot alternate the needle between north and south.
        assertEquals(0f, screenRelativeHeadingDegrees(pitchedAboutEastAxis(144), 0)!!, 0.01f)
        assertNull(screenRelativeHeadingDegrees(pitchedAboutEastAxis(144), 0, CompassRecoverHorizontalStrength))
        assertNull(screenRelativeHeadingDegrees(pitchedAboutEastAxis(153), 0, CompassRecoverHorizontalStrength))
        assertEquals(0f, screenRelativeHeadingDegrees(pitchedAboutEastAxis(135), 0, CompassRecoverHorizontalStrength)!!, 0.01f)
        assertEquals(180f, screenRelativeHeadingDegrees(pitchedAboutEastAxis(155), 0, CompassRecoverHorizontalStrength)!!, 0.01f)
    }

    @Test
    fun flatRollDoesNotBendTheHeading() {
        // Flat phone, top toward north, rolled about the top-edge axis: the top edge hasn't
        // moved, so the heading must stay exactly north (a magnitude-weighted camera blend
        // would bend this by ~14° at 30° of roll).
        for (rollDegrees in intArrayOf(15, 30, 60)) {
            val c = Math.cos(Math.toRadians(rollDegrees.toDouble())).toFloat()
            val s = Math.sin(Math.toRadians(rollDegrees.toDouble())).toFloat()
            val flatRolled = floatArrayOf(c, 0f, s, 0f, 1f, 0f, -s, 0f, c)
            assertEquals("roll $rollDegrees°", 0f, screenRelativeHeadingDegrees(flatRolled, 0)!!, 0.01f)
        }
    }

    @Test
    fun uprightRollKeepsTheHeadingNearTheCamera() {
        // Upright facing north, leaned 30° sideways: the camera still faces north, and the
        // tilted top edge may only pull the reading a few degrees.
        val uprightRolled30 = floatArrayOf(0.866f, -0.5f, 0f, 0f, 0f, -1f, 0.5f, 0.866f, 0f)
        val heading = screenRelativeHeadingDegrees(uprightRolled30, 0)!!
        assertTrue("leaned too far: $heading", angularDistanceDegrees(heading, 0f) < 5f)
    }

    @Test
    fun nonFiniteRotationMatrixReturnsNull() {
        assertNull(screenRelativeHeadingDegrees(FloatArray(9) { Float.NaN }, 0))
        assertNull(screenRelativeHeadingDegrees(FloatArray(9) { Float.POSITIVE_INFINITY }, 0))
    }

    @Test
    fun finiteVectorGuardCatchesEveryComponent() {
        assertTrue(isFiniteVector(floatArrayOf(0f, -9.8f, 3.2f)))
        assertTrue(!isFiniteVector(floatArrayOf(1f, 2f)))
        assertTrue(!isFiniteVector(floatArrayOf(1f), count = -1))
        for (index in 0..2) {
            for (bad in floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
                val values = floatArrayOf(1f, 2f, 3f)
                values[index] = bad
                assertTrue("index $index value $bad", !isFiniteVector(values))
            }
        }
        // Only the first `count` components matter (sensor arrays can carry extra slots).
        assertTrue(isFiniteVector(floatArrayOf(1f, 2f, 3f, Float.NaN), count = 3))
    }

    @Test
    fun displayRotationRemapsTheScreenUpAxis() {
        // One physical attitude (flat, device top pointing east): the on-screen heading must
        // follow the *screen's* up edge in every orientation of the UI.
        assertEquals(90f, screenRelativeHeadingDegrees(flatTopEast, 0)!!, 0.01f)
        assertEquals(0f, screenRelativeHeadingDegrees(flatTopEast, 90)!!, 0.01f)
        assertEquals(270f, screenRelativeHeadingDegrees(flatTopEast, 180)!!, 0.01f)
        assertEquals(180f, screenRelativeHeadingDegrees(flatTopEast, 270)!!, 0.01f)
    }

    @Test
    fun uprightLandscapeHeadingFollowsBackCamera() {
        // Held landscape and upright, camera facing east. In ROTATION_90 the screen-up axis is
        // the device −X axis (pointing at the sky, no horizontal component), so the camera is
        // the reference; in ROTATION_270 it is +X. Both must give the camera's azimuth.
        val landscape90UprightEast = floatArrayOf(0f, 0f, -1f, 0f, -1f, 0f, -1f, 0f, 0f)
        val landscape270UprightEast = floatArrayOf(0f, 0f, -1f, 0f, 1f, 0f, 1f, 0f, 0f)
        assertEquals(90f, screenRelativeHeadingDegrees(landscape90UprightEast, 90)!!, 0.01f)
        assertEquals(90f, screenRelativeHeadingDegrees(landscape270UprightEast, 270)!!, 0.01f)
    }

    // --- heading filter ---

    @Test
    fun filterSeedsImmediatelyFromFirstSample() {
        val filter = HeadingFilter()
        assertNull(filter.current)
        assertEquals(123.4f, filter.filter(123.4f, 0L), 1e-4f)
        assertEquals(123.4f, filter.current!!, 1e-4f)
    }

    @Test
    fun filterConvergesToAConstantInput() {
        val filter = HeadingFilter()
        var t = 0L
        filter.filter(100f, t)
        var out = 0f
        repeat(300) {
            t += StepNanos
            out = filter.filter(140f, t)
        }
        assertEquals(140f, out, 2f)
    }

    @Test
    fun filterCrossesNorthTheShortWay() {
        val filter = HeadingFilter()
        var t = 0L
        filter.filter(350f, t)
        repeat(300) {
            t += StepNanos
            val out = filter.filter(10f, t)
            // Staying on the short arc means never more than 20° from north; the long way
            // would sweep through 180°.
            assertTrue("left the short arc: $out", angularDistanceDegrees(out, 0f) <= 20.01f)
        }
        assertEquals(0f, angularDistanceDegrees(filter.current!!, 10f), 2f)
    }

    @Test
    fun filterSuppressesJitterAtRest() {
        val filter = HeadingFilter()
        var t = 0L
        filter.filter(100f, t)
        repeat(200) { index ->
            t += StepNanos
            val noisy = if (index % 2 == 0) 103f else 97f
            val out = filter.filter(noisy, t)
            assertTrue("jitter leaked through: $out", angularDistanceDegrees(out, 100f) <= 1.5f)
        }
    }

    @Test
    fun filterIgnoresASingleOutlierSpike() {
        val filter = HeadingFilter()
        var t = 0L
        filter.filter(100f, t)
        repeat(10) {
            t += StepNanos
            filter.filter(100f, t)
        }
        t += StepNanos
        val duringSpike = filter.filter(260f, t)
        assertEquals(0f, angularDistanceDegrees(duringSpike, 100f), 1f)
        repeat(10) {
            t += StepNanos
            filter.filter(100f, t)
        }
        assertEquals(0f, angularDistanceDegrees(filter.current!!, 100f), 1f)
    }

    @Test
    fun filterAcceptsAGenuineJumpAfterConsecutiveAgreement() {
        val filter = HeadingFilter()
        var t = 0L
        filter.filter(100f, t)
        repeat(10) {
            t += StepNanos
            filter.filter(100f, t)
        }
        var out = 0f
        repeat(3) {
            t += StepNanos
            out = filter.filter(260f, t)
        }
        assertEquals(260f, out, 1e-3f)
    }

    @Test
    fun filterTracksFastRotationWithSmallLag() {
        val filter = HeadingFilter()
        var t = 0L
        var input = 0f
        filter.filter(input, t)
        repeat(150) {
            t += StepNanos
            input = normalizeDegrees(input + 2f) // 100°/s
            filter.filter(input, t)
        }
        assertTrue(
            "lag too large: ${angularDistanceDegrees(filter.current!!, input)}°",
            angularDistanceDegrees(filter.current!!, input) < 10f,
        )
    }

    @Test
    fun filterResetForgetsTheEstimate() {
        val filter = HeadingFilter()
        filter.filter(100f, 0L)
        filter.reset()
        assertNull(filter.current)
        assertEquals(250f, filter.filter(250f, StepNanos), 1e-4f)
    }

    @Test
    fun filterRejectsNonFiniteSamplesAndRecovers() {
        val filter = HeadingFilter()
        filter.filter(Float.NaN, 0L)
        assertNull(filter.current) // a NaN must not seed the estimate
        filter.filter(100f, StepNanos)
        assertEquals(100f, filter.current!!, 1e-4f)
        filter.filter(Float.NaN, 2 * StepNanos)
        filter.filter(Float.POSITIVE_INFINITY, 3 * StepNanos)
        filter.filter(Float.NEGATIVE_INFINITY, 4 * StepNanos)
        assertEquals(100f, filter.current!!, 1e-4f) // and must not poison it
        val recovered = filter.filter(104f, 5 * StepNanos)
        assertTrue("did not resume filtering: $recovered", recovered > 100f && recovered < 104f)
    }

    @Test
    fun filterIgnoresNonMonotonicTimestamps() {
        // The accel+mag fallback interleaves two sensor clocks; a backwards timestamp must not
        // be turned into a fake speed spike.
        val filter = HeadingFilter()
        filter.filter(100f, 1_000_000_000L)
        assertEquals(100f, filter.filter(150f, 900_000_000L), 1e-4f)
        assertEquals(100f, filter.current!!, 1e-4f)
    }

    // --- alignment gate ---

    @Test
    fun gateGivesNoGuidanceWithoutAHeading() {
        val gate = AlignmentGate()
        assertNull(gate.update(null, degraded = false))
        assertEquals(AlignmentDirection.Aligned, gate.update(0f, degraded = false))
        assertNull(gate.update(null, degraded = false)) // losing the heading drops alignment
    }

    @Test
    fun gateAlignsWithHysteresisAcrossTheThreshold() {
        val gate = AlignmentGate(enterDegrees = 5f, exitDegrees = 8f)
        assertEquals(AlignmentDirection.TurnRight, gate.update(6.5f, degraded = false)) // not yet in
        assertEquals(AlignmentDirection.Aligned, gate.update(4f, degraded = false))
        assertEquals(AlignmentDirection.Aligned, gate.update(6.5f, degraded = false)) // noise stays in
        assertEquals(AlignmentDirection.TurnRight, gate.update(9f, degraded = false)) // genuinely out
        assertEquals(AlignmentDirection.TurnRight, gate.update(6.5f, degraded = false)) // must re-enter ≤5
        assertEquals(AlignmentDirection.Aligned, gate.update(3f, degraded = false))
    }

    @Test
    fun gateNeverClaimsAlignmentWhileDegraded() {
        val gate = AlignmentGate()
        assertNull(gate.update(0f, degraded = true)) // pointing at it, but not trustworthy
        assertNull(gate.update(90f, degraded = true)) // no directional claim while degraded
        assertEquals(AlignmentDirection.Aligned, gate.update(0f, degraded = false)) // recovery
    }

    @Test
    fun degradedInputIsIdempotentInsideTheHysteresisBand() {
        val gate = AlignmentGate(enterDegrees = 5f, exitDegrees = 8f)
        assertEquals(AlignmentDirection.Aligned, gate.update(4f, degraded = false))
        assertNull(gate.update(6.5f, degraded = true))
        assertNull(gate.update(6.5f, degraded = true))
    }

    @Test
    fun gateTurnsTheShorterWay() {
        val gate = AlignmentGate()
        assertEquals(AlignmentDirection.TurnRight, gate.update(90f, degraded = false))
        assertEquals(AlignmentDirection.TurnLeft, gate.update(270f, degraded = false))
        assertEquals(AlignmentDirection.TurnLeft, gate.update(-20f, degraded = false)) // unnormalized input
    }
}
