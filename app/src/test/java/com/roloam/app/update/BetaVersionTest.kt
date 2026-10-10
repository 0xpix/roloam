package com.roloam.app.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BetaVersionTest {
    @org.junit.Test fun missedBetaNotesAreShownInOrder() {
        val change = buildUpdateChangelog("0.6.0-beta", "0.7.0-beta", listOf(
            "0.7.0-beta" to "New dashboard",
            "0.6.1-beta" to "Map bug fixes",
            "0.6.0-beta" to "Already installed"
        ))
        org.junit.Assert.assertTrue(change.indexOf("0.6.1-beta") < change.indexOf("0.7.0-beta"))
        org.junit.Assert.assertTrue(change.contains("Map bug fixes"))
        org.junit.Assert.assertFalse(change.contains("Already installed"))
    }

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
