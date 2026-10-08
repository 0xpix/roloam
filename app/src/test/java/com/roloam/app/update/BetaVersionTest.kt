package com.roloam.app.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BetaVersionTest {
    @Test
    fun simpleBetaMinorVersionWins() {
        assertTrue(isNewerBetaVersion("0.2.0-beta", "0.1.0-beta.2"))
    }

    @Test
    fun simpleBetaPatchVersionWins() {
        assertTrue(isNewerBetaVersion("0.2.1-beta", "0.2.0-beta"))
    }

    @Test
    fun equalSimpleBetaIsNotAnUpdate() {
        assertFalse(isNewerBetaVersion("0.2.0-beta", "0.2.0-beta"))
    }

    @Test
    fun oldNumberedBetaCompatibilityStillWorks() {
        assertTrue(isNewerBetaVersion("0.1.0-beta.3", "0.1.0-beta.2"))
    }

    @Test
    fun olderBaseVersionIsRejectedEvenWithHigherBetaRevision() {
        assertFalse(isNewerBetaVersion("0.1.9-beta.99", "0.2.0-beta"))
    }
}
