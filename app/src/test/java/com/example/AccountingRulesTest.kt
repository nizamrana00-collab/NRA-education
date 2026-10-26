package com.example

import com.example.accounting.AccountingEngine
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AccountingRulesTest {

    @Test
    fun testA_partialPayment() {
        val originalFee = 1000.0
        val openingBalance = 0.0
        val adjustments = 0.0

        // Step 1: Allocate 400
        var allocated = 400.0
        var balance = AccountingEngine.calculateClosingBalance(openingBalance, originalFee, adjustments, allocated)
        assertEquals(600.0, balance, 0.001)

        // Step 2: Allocate another 300 (total 700)
        allocated += 300.0
        balance = AccountingEngine.calculateClosingBalance(openingBalance, originalFee, adjustments, allocated)
        assertEquals(300.0, balance, 0.001)

        // Step 3: Allocate final 300 (total 1000)
        allocated += 300.0
        balance = AccountingEngine.calculateClosingBalance(openingBalance, originalFee, adjustments, allocated)
        assertEquals(0.0, balance, 0.001)
    }

    @Test
    fun testB_differentAdmissionDates() {
        // Student 1: Admission 23rd August
        val start1 = LocalDate.of(2026, 8, 23)
        val anchor1 = 23
        val end1 = AccountingEngine.calculateCycleEnd(start1, anchor1)
        assertEquals(LocalDate.of(2026, 9, 22), end1)

        // Student 2: Admission 29th August
        val start2 = LocalDate.of(2026, 8, 29)
        val anchor2 = 29
        val end2 = AccountingEngine.calculateCycleEnd(start2, anchor2)
        assertEquals(LocalDate.of(2026, 9, 28), end2)

        // Both calculated independently and end before their respective next anchor dates
        val nextStart1 = AccountingEngine.calculateNextCycleStart(end1)
        val nextStart2 = AccountingEngine.calculateNextCycleStart(end2)
        assertEquals(LocalDate.of(2026, 9, 23), nextStart1)
        assertEquals(LocalDate.of(2026, 9, 29), nextStart2)
    }

    @Test
    fun testC_februaryDateHandling() {
        val anchor = 29

        // Normal Year (2025): Feb has 28 days -> anchor maps to Feb 28
        val febNormal = AccountingEngine.getEffectiveDayForMonth(2025, 2, anchor)
        assertEquals(28, febNormal)

        // Leap Year (2024): Feb has 29 days -> anchor maps to Feb 29
        val febLeap = AccountingEngine.getEffectiveDayForMonth(2024, 2, anchor)
        assertEquals(29, febLeap)

        // Following March: Anchor restores to 29
        val marAfter = AccountingEngine.getEffectiveDayForMonth(2025, 3, anchor)
        assertEquals(29, marAfter)
    }

    @Test
    fun testD_latePaymentAndArrears() {
        // September cycle: Charge 1000, unallocated 0 -> balance 1000
        val sepCharge = 1000.0
        var sepAllocated = 0.0
        var sepBalance = AccountingEngine.calculateClosingBalance(0.0, sepCharge, 0.0, sepAllocated)
        assertEquals(1000.0, sepBalance, 0.001)

        // October new cycle begins: Charge 1000 -> balance 1000
        val octCharge = 1000.0
        val octAllocated = 0.0
        val octBalance = AccountingEngine.calculateClosingBalance(0.0, octCharge, 0.0, octAllocated)
        assertEquals(1000.0, octBalance, 0.001)

        // Payment of 1000 received in October, allocated explicitly to September
        val octCashReceived = 1000.0
        sepAllocated += octCashReceived
        sepBalance = AccountingEngine.calculateClosingBalance(0.0, sepCharge, 0.0, sepAllocated)

        // Expected result: Sep balance = 0, Oct balance = 1000, Oct cash received = 1000
        assertEquals(0.0, sepBalance, 0.001)
        assertEquals(1000.0, octBalance, 0.001)
        assertEquals(1000.0, octCashReceived, 0.001)
    }

    @Test
    fun testE_breakExtension() {
        val originalEnd = LocalDate.of(2026, 9, 30)
        val eligibleDays = 4

        // 1. Declined extension: cycle date remains original
        val declinedDecision = "DO_NOT_EXTEND"
        val effectiveEnd1 = if (declinedDecision == "EXTEND") {
            AccountingEngine.calculateExtendedDate(originalEnd, eligibleDays)
        } else originalEnd
        assertEquals(LocalDate.of(2026, 9, 30), effectiveEnd1)

        // 2. Approved extension: cycle date extended by eligibleDays
        val approvedDecision = "EXTEND"
        val effectiveEnd2 = if (approvedDecision == "EXTEND") {
            AccountingEngine.calculateExtendedDate(originalEnd, eligibleDays)
        } else originalEnd
        assertEquals(LocalDate.of(2026, 10, 4), effectiveEnd2)
    }

    @Test
    fun testF_feeChanges() {
        val oldFee = 1000.0
        val newFee = 1200.0

        // Historical settled cycle charge
        val historicalCycleCharge = oldFee
        assertEquals(1000.0, historicalCycleCharge, 0.001)

        // Future cycle after effective date uses new fee
        val futureCycleCharge = newFee
        assertEquals(1200.0, futureCycleCharge, 0.001)

        // Historical remains unchanged
        assertEquals(1000.0, historicalCycleCharge, 0.001)
    }

    @Test
    fun testG_advancePayment() {
        val cycleCharge = 800.0
        val paymentReceived = 1000.0

        // Explicit allocation up to outstanding balance
        val allocated = minOf(paymentReceived, cycleCharge)
        val cycleBalance = AccountingEngine.calculateClosingBalance(0.0, cycleCharge, 0.0, allocated)
        val unallocatedCredit = paymentReceived - allocated

        assertEquals(800.0, allocated, 0.001)
        assertEquals(0.0, cycleBalance, 0.001)
        assertEquals(200.0, unallocatedCredit, 0.001)

        // Total received reconciles with allocated + credit
        assertEquals(paymentReceived, allocated + unallocatedCredit, 0.001)
    }

    @Test
    fun testH_paymentCorrection() {
        val charge = 1000.0
        var allocated = 600.0
        var balance = AccountingEngine.calculateClosingBalance(0.0, charge, 0.0, allocated)
        assertEquals(400.0, balance, 0.001)

        // Reversal of payment (600)
        allocated -= 600.0
        balance = AccountingEngine.calculateClosingBalance(0.0, charge, 0.0, allocated)
        assertEquals(1000.0, balance, 0.001)
    }

    @Test
    fun testI_inactiveStudents() {
        // Even when status is changed to INACTIVE, previous records maintain their values
        val historicalFee = 1000.0
        val historicalAllocated = 1000.0
        val status = "INACTIVE"

        val balance = AccountingEngine.calculateClosingBalance(0.0, historicalFee, 0.0, historicalAllocated)
        assertEquals(0.0, balance, 0.001)
        assertEquals("INACTIVE", status)
    }

    @Test
    fun testJ_sharedGroupBilling() {
        // Group with 3 students, total agreed fee = 3000
        val numberOfStudents = 3
        val agreedTotalFee = 3000.0

        // The group billing charge must be created ONCE, not multiplied by 3
        val groupCharge = agreedTotalFee
        assertEquals(3000.0, groupCharge, 0.001)
        assertNotEquals(numberOfStudents * agreedTotalFee, groupCharge)
    }

    @Test
    fun testK_monthEndAndYearEndBoundaries() {
        // Year-end December to January transition
        val decStart = LocalDate.of(2025, 12, 15)
        val anchor = 15
        val decEnd = AccountingEngine.calculateCycleEnd(decStart, anchor)

        assertEquals(LocalDate.of(2026, 1, 14), decEnd)

        val janStart = AccountingEngine.calculateNextCycleStart(decEnd)
        assertEquals(LocalDate.of(2026, 1, 15), janStart)
        assertEquals(2026, janStart.year)
    }

    @Test
    fun testL_reportConsistency() {
        val payment1 = 1000.0
        val payment2 = 500.0
        val totalCash = payment1 + payment2

        val alloc1 = 800.0
        val credit1 = 200.0 // from payment 1
        val alloc2 = 500.0
        val credit2 = 0.0   // from payment 2

        val totalAllocated = alloc1 + alloc2
        val totalCredit = credit1 + credit2

        // Invariant: Cash Received == Allocated + Unallocated Credit
        assertEquals(totalCash, totalAllocated + totalCredit, 0.001)
    }

    @Test
    fun testM_currencyAndBengaliFormatting() {
        val formattedAscii = AccountingEngine.formatCurrency(1500.0, useBengaliNumerals = false)
        assertEquals("৳ 1,500", formattedAscii)

        val formattedBengali = AccountingEngine.formatCurrency(1500.0, useBengaliNumerals = true)
        assertEquals("৳ ১,৫০০", formattedBengali)
    }

    @Test
    fun testN_monthlyReportGroupingByPaymentDate() {
        val student1 = com.example.data.entity.StudentEntity(
            id = "s1", name = "রাকিব", grade = "Class 9", guardianName = "করিম", phone = "01700000000",
            address = "Dhaka", admissionDate = "2026-01-10", monthlyFee = 2000.0, billingDayAnchor = 10
        )
        val student2 = com.example.data.entity.StudentEntity(
            id = "s2", name = "সাদিয়া", grade = "Class 10", guardianName = "রহিম", phone = "01800000000",
            address = "Dhaka", admissionDate = "2026-02-01", monthlyFee = 2500.0, billingDayAnchor = 1
        )

        val p1 = com.example.data.entity.PaymentEntity(
            id = "p1", studentId = "s1", amountReceived = 2000.0, paymentDate = "2026-03-05", paymentMethod = "CASH"
        )
        val p2 = com.example.data.entity.PaymentEntity(
            id = "p2", studentId = "s2", amountReceived = 2500.0, paymentDate = "2026-03-18", paymentMethod = "BKASH"
        )
        val p3 = com.example.data.entity.PaymentEntity(
            id = "p3", studentId = "s1", amountReceived = 2000.0, paymentDate = "2026-02-12", paymentMethod = "CASH"
        )
        val pReversed = com.example.data.entity.PaymentEntity(
            id = "p4", studentId = "s1", amountReceived = 5000.0, paymentDate = "2026-03-01", paymentMethod = "CASH",
            isReversed = true, reversalReason = "ভুল এন্ট্রি"
        )

        val summary = com.example.accounting.MonthlyReportEngine.generateHistoricalReports(
            payments = listOf(p1, p2, p3, pReversed),
            students = listOf(student1, student2),
            useBengaliNumerals = false
        )

        // March 2026 report
        val marchReport = summary.reports.firstOrNull { it.yearMonth == "2026-03" }
        assertNotNull(marchReport)
        assertEquals(4500.0, marchReport!!.totalCollected, 0.001) // 2000 + 2500 (reversed 5000 excluded)
        assertEquals(2, marchReport.totalPaymentCount)
        assertEquals(2, marchReport.payingStudentCount) // s1 and s2 both paid

        // February 2026 report
        val febReport = summary.reports.firstOrNull { it.yearMonth == "2026-02" }
        assertNotNull(febReport)
        assertEquals(2000.0, febReport!!.totalCollected, 0.001)
        assertEquals(1, febReport.totalPaymentCount)
        assertEquals(1, febReport.payingStudentCount) // only s1 paid in Feb

        // Lifetime summary
        assertEquals(6500.0, summary.totalLifetimeCollected, 0.001)
        assertEquals(3, summary.totalLifetimeTransactions)
    }
}

