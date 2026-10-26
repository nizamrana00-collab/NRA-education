package com.example.accounting

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Authoritative Accounting Engine for "আমার টিউশন হিসাব".
 * Centralizes all financial formulas, billing cycle anchor rules,
 * break extensions, and reconciliation logic.
 */
object AccountingEngine {

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val DISPLAY_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    /**
     * Exact 2-decimal monetary rounding to prevent floating point drift.
     */
    fun roundMoney(amount: Double): Double {
        return (amount * 100.0).roundToLong() / 100.0
    }

    /**
     * Rule A & B: Monthly billing day anchor calculation.
     * Given an anchor day (e.g. 23 or 29) and a target year-month,
     * calculates the effective day for that month.
     * E.g. Anchor 29 in Feb 2025 -> Feb 28; Feb 2024 -> Feb 29; Mar 2024 -> Mar 29.
     */
    fun getEffectiveDayForMonth(year: Int, month: Int, anchorDay: Int): Int {
        val ym = YearMonth.of(year, month)
        val lengthOfMonth = ym.lengthOfMonth()
        return if (anchorDay > lengthOfMonth) lengthOfMonth else anchorDay
    }

    /**
     * Calculate billing cycle range anchored to [anchorDay].
     * 
     * Core Accounting Invariant ("প্রতিটি সাইকেল সর্বোচ্চ পুরো ১ মাস"):
     * Under normal conditions, every single cycle is strictly a full 1-month period:
     * from [cycleStartDate] to [cycleStartDate.plusMonths(1).minusDays(1)].
     * It NEVER exceeds 1 full month.
     *
     * The ONLY time a cycle period extends and due date is pushed back is when
     * an approved holiday/break is recorded (see [calculateExtendedDate]).
     */
    fun calculateCycleEnd(cycleStartDate: LocalDate, anchorDay: Int = cycleStartDate.dayOfMonth): LocalDate {
        // Every cycle is strictly a full 1 calendar month:
        // from cycleStartDate to cycleStartDate.plusMonths(1).minusDays(1).
        // It NEVER exceeds 1 month, and never gets cut short.
        return cycleStartDate.plusMonths(1).minusDays(1)
    }

    /**
     * Calculate next cycle start date: exactly 1 day after the previous cycle ends.
     */
    fun calculateNextCycleStart(previousCycleEnd: LocalDate): LocalDate {
        return previousCycleEnd.plusDays(1)
    }

    /**
     * Mandatory Cycle Balance Formula:
     * Closing outstanding balance = Opening balance + Valid charges + Adjustments - Allocated payments
     */
    fun calculateClosingBalance(
        openingBalance: Double,
        charge: Double,
        adjustments: Double,
        allocatedPayments: Double
    ): Double {
        val totalDue = openingBalance + charge + adjustments
        val balance = totalDue - allocatedPayments
        return roundMoney(if (balance < 0.0) 0.0 else balance)
    }

    /**
     * Calculate break extension:
     * Adds [eligibleDays] to the current cycle end date.
     */
    fun calculateExtendedDate(originalEndDate: LocalDate, eligibleDays: Int): LocalDate {
        require(eligibleDays >= 0) { "Eligible days cannot be negative" }
        return originalEndDate.plusDays(eligibleDays.toLong())
    }

    /**
     * Bengali currency formatting (e.g. ৳ ১,৫০০ or ৳ 1,500).
     */
    fun formatCurrency(amount: Double, useBengaliNumerals: Boolean = false): String {
        val rounded = roundMoney(amount)
        val isNegative = rounded < 0
        val absVal = kotlin.math.abs(rounded)
        val formattedNumber = if (absVal % 1.0 == 0.0) {
            String.format(Locale.US, "%,d", absVal.toLong())
        } else {
            String.format(Locale.US, "%,.2f", absVal)
        }
        val numString = if (useBengaliNumerals) toBengaliDigits(formattedNumber) else formattedNumber
        return if (isNegative) "-৳ $numString" else "৳ $numString"
    }

    /**
     * Converts ASCII digits to Bengali digits.
     */
    fun toBengaliDigits(input: String): String {
        val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(bengaliDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Format LocalDate to DD-MM-YYYY in Bengali or ASCII numerals.
     */
    fun formatDate(date: LocalDate, useBengaliNumerals: Boolean = false): String {
        val str = date.format(DISPLAY_DATE_FORMATTER)
        return if (useBengaliNumerals) toBengaliDigits(str) else str
    }

    fun parseDate(dateStr: String): LocalDate {
        return LocalDate.parse(dateStr, DATE_FORMATTER)
    }
}
