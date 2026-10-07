package com.roloam.app.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BetaVersionTest {
    @Test
    fun newerBetaSequenceWins() {
        assertTrue(isNewerBetaVersion("0.1.0-beta.2", "0.1.0-beta.1"))
    }

    @Test
    fun equalBetaIsNotAnUpdate() {
        assertFalse(isNewerBetaVersion("0.1.0-beta.1", "0.1.0-beta.1"))
    }

    @Test
    fun newerBaseVersionWins() {
        assertTrue(isNewerBetaVersion("0.2.0-beta.1", "0.1.9-beta.99"))
    }

    @Test
    fun olderReleaseIsRejected() {
        assertFalse(isNewerBetaVersion("0.1.0-beta.3", "0.1.1-beta.1"))
    }
}
