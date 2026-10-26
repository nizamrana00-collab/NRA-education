package com.example

import com.example.accounting.AccountingEngine
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Authoritative Accounting Tests for NRA Education Center.
 * Verifies cycle anchor calculations, leap years, balance formulas,
 * break extensions, and Bengali currency formatting.
 */
class ExampleUnitTest {

    @Test
    fun testCycleEnd_firstOfMonthAnchor() {
        // When anchor is 1st of the month, cycle runs from 1st to last day of that same month
        val start = LocalDate.of(2025, 8, 1)
        val end = AccountingEngine.calculateCycleEnd(start, anchorDay = 1)
        assertEquals(LocalDate.of(2025, 8, 31), end)
    }

    @Test
    fun testCycleEnd_midMonthAnchor23() {
        // When anchor is 23rd, cycle runs from Aug 23 to Sep 22
        val start = LocalDate.of(2025, 8, 23)
        val end = AccountingEngine.calculateCycleEnd(start, anchorDay = 23)
        assertEquals(LocalDate.of(2025, 9, 22), end)
    }

    @Test
    fun testCycleEnd_anchor29InFebruaryLeapYear() {
        // In 2024 (leap year), Feb has 29 days.
        // Cycle starting Jan 29, anchor 29 -> next renewal is Feb 29, so cycle ends Feb 28
        val start = LocalDate.of(2024, 1, 29)
        val end = AccountingEngine.calculateCycleEnd(start, anchorDay = 29)
        assertEquals(LocalDate.of(2024, 2, 28), end)
    }

    @Test
    fun testCycleEnd_anchor29InFebruaryNonLeapYear() {
        // In 2025 (non-leap year), Feb has 28 days.
        // Effective anchor day in Feb is 28, so cycle ends Feb 27
        val start = LocalDate.of(2025, 1, 29)
        val end = AccountingEngine.calculateCycleEnd(start, anchorDay = 29)
        assertEquals(LocalDate.of(2025, 2, 27), end)
    }

    @Test
    fun testNextCycleStart() {
        val prevEnd = LocalDate.of(2025, 8, 31)
        val nextStart = AccountingEngine.calculateNextCycleStart(prevEnd)
        assertEquals(LocalDate.of(2025, 9, 1), nextStart)
    }

    @Test
    fun testClosingBalance_fullPayment() {
        // Opening balance 500, fee 1500, adjustments 0, allocated 2000 -> Closing balance 0
        val closing = AccountingEngine.calculateClosingBalance(
            openingBalance = 500.0,
            charge = 1500.0,
            adjustments = 0.0,
            allocatedPayments = 2000.0
        )
        assertEquals(0.0, closing, 0.001)
    }

    @Test
    fun testClosingBalance_partialPayment() {
        // Opening balance 0, fee 2000, adjustments 0, allocated 1200 -> Closing balance 800
        val closing = AccountingEngine.calculateClosingBalance(
            openingBalance = 0.0,
            charge = 2000.0,
            adjustments = 0.0,
            allocatedPayments = 1200.0
        )
        assertEquals(800.0, closing, 0.001)
    }

    @Test
    fun testClosingBalance_neverNegativeOnOverAllocation() {
        // Allocated > total due: closing balance is bounded at 0.0
        val closing = AccountingEngine.calculateClosingBalance(
            openingBalance = 0.0,
            charge = 1000.0,
            adjustments = 0.0,
            allocatedPayments = 1500.0
        )
        assertEquals(0.0, closing, 0.001)
    }

    @Test
    fun testBreakExtension_sevenDays() {
        // Cycle ends Sep 22, student has 7 days approved holiday -> cycle extended to Sep 29
        val originalEnd = LocalDate.of(2025, 9, 22)
        val extendedEnd = AccountingEngine.calculateExtendedDate(originalEnd, eligibleDays = 7)
        assertEquals(LocalDate.of(2025, 9, 29), extendedEnd)
    }

    @Test
    fun testRoundMoney_preventsFloatingPointDrift() {
        // E.g. 1000.10 + 2000.20 in IEEE-754 can be 3000.3000000000004
        val unrounded = 1000.10 + 2000.20
        val rounded = AccountingEngine.roundMoney(unrounded)
        assertEquals(3000.30, rounded, 0.0001)
    }

    @Test
    fun testBengaliNumerals() {
        val bengali = AccountingEngine.toBengaliDigits("1234567890")
        assertEquals("১২৩৪৫৬৭৮৯০", bengali)
    }

    @Test
    fun testCurrencyFormatting() {
        val formattedAscii = AccountingEngine.formatCurrency(1500.0, useBengaliNumerals = false)
        assertEquals("৳ 1,500", formattedAscii)

        val formattedBengali = AccountingEngine.formatCurrency(1500.0, useBengaliNumerals = true)
        assertEquals("৳ ১,৫০০", formattedBengali)
    }

    @Test
    fun testCycleNeverExceedsOneMonth_standardAndShiftedDates() {
        // Core User Invariant: "protita cycle full 1 mash er beshi hobee na"
        val testStarts = listOf(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 15),
            LocalDate.of(2025, 1, 23),
            LocalDate.of(2025, 1, 29),
            LocalDate.of(2025, 2, 6),  // Shifted start after holiday
            LocalDate.of(2025, 9, 28), // Shifted start after holiday
            LocalDate.of(2025, 10, 10),
            LocalDate.of(2025, 12, 31)
        )

        for (start in testStarts) {
            val end = AccountingEngine.calculateCycleEnd(start, anchorDay = 23)
            val maxAllowedEnd = start.plusMonths(1).minusDays(1)
            // Cycle end must never be after maxAllowedEnd (cannot exceed 1 full month)
            assertTrue("Cycle starting on $start ended on $end which exceeds $maxAllowedEnd", !end.isAfter(maxAllowedEnd))
            assertTrue("Cycle ending $end cannot be before start $start", !end.isBefore(start))
        }
    }

    @Test
    fun testHolidayExtension_pushesCycleEndDateAndNextCycleStaysOneMonth() {
        // Cycle 1: 2025-08-23 to 2025-09-22
        val cycle1Start = LocalDate.of(2025, 8, 23)
        val cycle1End = AccountingEngine.calculateCycleEnd(cycle1Start, anchorDay = 23)
        assertEquals(LocalDate.of(2025, 9, 22), cycle1End)

        // Holiday given: 5 days break approved -> date pushed back to Sep 27
        val extendedCycle1End = AccountingEngine.calculateExtendedDate(cycle1End, eligibleDays = 5)
        assertEquals(LocalDate.of(2025, 9, 27), extendedCycle1End)

        // Cycle 2 starts 1 day after extended end date
        val cycle2Start = AccountingEngine.calculateNextCycleStart(extendedCycle1End)
        assertEquals(LocalDate.of(2025, 9, 28), cycle2Start)

        // Cycle 2 MUST be strictly a full 1 month: 2025-09-28 to 2025-10-27
        val cycle2End = AccountingEngine.calculateCycleEnd(cycle2Start, anchorDay = 23)
        assertEquals(LocalDate.of(2025, 10, 27), cycle2End)

        // Verify Cycle 2 does not exceed 1 full month
        val maxAllowedCycle2End = cycle2Start.plusMonths(1).minusDays(1)
        assertEquals(maxAllowedCycle2End, cycle2End)
    }

    @Test
    fun testHolidayExtension_twentyDaysBreak() {
        // Cycle: Jan 1 to Jan 31
        val start = LocalDate.of(2025, 1, 1)
        val end = AccountingEngine.calculateCycleEnd(start, anchorDay = 1)
        assertEquals(LocalDate.of(2025, 1, 31), end)

        // 20 days vacation
        val extendedEnd = AccountingEngine.calculateExtendedDate(end, eligibleDays = 20)
        assertEquals(LocalDate.of(2025, 2, 20), extendedEnd)

        // Next cycle starts Feb 21, runs strictly for 1 full month (Feb 21 to Mar 20)
        val nextStart = AccountingEngine.calculateNextCycleStart(extendedEnd)
        assertEquals(LocalDate.of(2025, 2, 21), nextStart)
        val nextEnd = AccountingEngine.calculateCycleEnd(nextStart, anchorDay = 1)
        assertEquals(LocalDate.of(2025, 3, 20), nextEnd)
    }
}
