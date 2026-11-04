package com.roloam.app.ui

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class DestinationScrambleFramesTest {
    @Test fun everyLetterLocksInOrderAndPunctuationStaysFixed() {
        val destination = "Baden-Baden 2"
        val all = DestinationScrambleFrames.letterCount(destination)
        assertEquals(11, all)
        val initial = DestinationScrambleFrames.frame(destination, 0, Random(2))
        assertEquals(destination.length, initial.length)
        assertEquals('-', initial[5])
        assertEquals(' ', initial[11])
        assertNotEquals(destination, initial)

        val three = DestinationScrambleFrames.frame(destination, 3, Random(5))
        assertEquals("Bad", three.take(3))
        assertEquals('-', three[5])
        assertEquals(destination, DestinationScrambleFrames.frame(destination, all, Random(6)))
    }

    @Test fun preservesUnicodeDestinationLettersAndEmptyNames() {
        val name = "Würzburg"
        assertEquals(name, DestinationScrambleFrames.frame(name,
            DestinationScrambleFrames.letterCount(name), Random(3)))
        assertEquals("", DestinationScrambleFrames.frame("", 0, Random(1)))
    }
}
