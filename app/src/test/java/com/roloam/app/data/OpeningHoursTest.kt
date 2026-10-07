package com.roloam.app.data

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class OpeningHoursTest {
    private val monday = LocalDate.of(2026, 10, 5)
    private val saturday = LocalDate.of(2026, 10, 10)

    @Test
    fun twentyFourSevenIsOpen() {
        assertEquals(
            OpeningState.OPEN,
            openingState("24/7", monday, LocalTime.of(3, 15))
        )
    }

    @Test
    fun weekdayWindowIsRespected() {
        assertEquals(
            OpeningState.OPEN,
            openingState("Mo-Fr 09:00-18:00", monday, LocalTime.of(10, 0))
        )
        assertEquals(
            OpeningState.CLOSED,
            openingState("Mo-Fr 09:00-18:00", monday, LocalTime.of(19, 0))
        )
    }

    @Test
    fun weekendDaySelectionWorks() {
        assertEquals(
            OpeningState.OPEN,
            openingState("Sa 10:00-16:00; Su off", saturday, LocalTime.of(11, 30))
        )
    }

    @Test
    fun complexRulesStayUnknownInsteadOfBeingGuessed() {
        assertEquals(
            OpeningState.UNKNOWN,
            openingState("sunrise-sunset", monday, LocalTime.NOON)
        )
    }
}
