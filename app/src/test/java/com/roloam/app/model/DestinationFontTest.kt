package com.roloam.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DestinationFontTest {
    @Test fun selectedFontRoundTripsThroughPreferences() {
        DestinationFont.entries.forEach { chosen ->
            assertEquals(chosen, DestinationFont.fromStorage(chosen.name))
        }
    }

    @Test fun missingOrUnknownPreferenceUsesReadableDefault() {
        assertEquals(DestinationFont.GEIST_MONO, DestinationFont.fromStorage(null))
        assertEquals(DestinationFont.GEIST_MONO, DestinationFont.fromStorage("UNKNOWN"))
    }
}
