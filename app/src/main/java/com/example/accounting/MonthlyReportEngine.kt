package com.example.accounting

import com.example.data.entity.PaymentEntity
import com.example.data.entity.StudentEntity
import java.time.LocalDate
import java.time.YearMonth

/**
 * Detailed item for a single payment transaction associated with student info.
 */
data class MonthlyPaymentItem(
    val payment: PaymentEntity,
    val studentName: String,
    val studentPhone: String,
    val studentGrade: String,
    val formattedDate: String,
    val formattedAmount: String
)

/**
 * Aggregated monthly financial report containing all receipts, counts, and breakdown.
 */
data class MonthlyFinancialReport(
    val yearMonth: String, // "YYYY-MM", e.g. "2026-03"
    val year: Int,
    val month: Int,
    val monthNameBengali: String, // e.g. "মার্চ ২০২৬"
    val monthNameEnglish: String, // e.g. "March 2026"
    val totalCollected: Double,
    val totalPaymentCount: Int,
    val payingStudentCount: Int,
    val enrolledStudentCount: Int,
    val payments: List<MonthlyPaymentItem>,
    val paymentMethodBreakdown: Map<String, Double>
)

/**
 * Lifetime historical summary across all months since day 1.
 */
data class LifetimeFinancialSummary(
    val totalLifetimeCollected: Double,
    val totalLifetimeTransactions: Int,
    val totalMonthsWithData: Int,
    val totalRegisteredStudents: Int,
    val reports: List<MonthlyFinancialReport>
)

object MonthlyReportEngine {

    private val BENGALI_MONTHS = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    private val ENGLISH_MONTHS = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    /**
     * Format a YearMonth string (YYYY-MM) into Bengali representation, e.g. "মার্চ ২০২৬".
     */
    fun getBengaliMonthYear(year: Int, month: Int, useBengaliNumerals: Boolean): String {
        val monthName = if (month in 1..12) BENGALI_MONTHS[month - 1] else "মাস $month"
        val yearStr = if (useBengaliNumerals) AccountingEngine.toBengaliDigits(year.toString()) else year.toString()
        return "$monthName $yearStr"
    }

    /**
     * Format a YearMonth string (YYYY-MM) into English representation, e.g. "March 2026".
     */
    fun getEnglishMonthYear(year: Int, month: Int): String {
        val monthName = if (month in 1..12) ENGLISH_MONTHS[month - 1] else "Month $month"
        return "$monthName $year"
    }

    /**
     * Generate complete historical monthly financial reports grouped by the month of [PaymentEntity.paymentDate].
     * Guarantees all past payments from Day 1 are preserved and organized by month.
     */
    fun generateHistoricalReports(
        payments: List<PaymentEntity>,
        students: List<StudentEntity>,
        useBengaliNumerals: Boolean
    ): LifetimeFinancialSummary {
        val studentMap = students.associateBy { it.id }
        val today = LocalDate.now()
        val currentYmStr = String.format("%04d-%02d", today.year, today.monthValue)

        // Filter valid non-reversed payments
        val validPayments = payments.filter { !it.isReversed }

        // Extract all distinct year-months present in payments, plus always include the current month
        val allYearMonths = validPayments
            .mapNotNull { extractYearMonth(it.paymentDate) }
            .toMutableSet()
        allYearMonths.add(currentYmStr)

        // Sort descending (newest month first)
        val sortedYearMonths = allYearMonths.sortedDescending()

        val monthlyReports = sortedYearMonths.map { ymStr ->
            val parts = ymStr.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: today.year
            val month = parts.getOrNull(1)?.toIntOrNull() ?: today.monthValue

            // All payments received in this month based on paymentDate
            val monthPayments = validPayments
                .filter { extractYearMonth(it.paymentDate) == ymStr }
                .sortedByDescending { it.paymentDate }

            val totalCollected = AccountingEngine.roundMoney(monthPayments.sumOf { it.amountReceived })
            val totalPaymentCount = monthPayments.size

            // Distinct students who made a payment in this month
            val payingStudentIds = monthPayments.mapNotNull { it.studentId }.distinct()
            val payingStudentCount = payingStudentIds.size

            // Enrolled students up to the end of this month
            val monthEnd = try {
                YearMonth.of(year, month).atEndOfMonth().toString()
            } catch (e: Exception) {
                today.toString()
            }
            val enrolledStudentCount = students.count { stu ->
                stu.admissionDate <= monthEnd && (stu.status == "ACTIVE" || stu.status == "ON_BREAK")
            }.let { if (it == 0 && students.isNotEmpty()) students.size else it }

            // Breakdown by payment method
            val methodBreakdown = monthPayments
                .groupBy { it.paymentMethod }
                .mapValues { (_, list) -> AccountingEngine.roundMoney(list.sumOf { it.amountReceived }) }

            // Map payment items with student information
            val paymentItems = monthPayments.map { pay ->
                val stu = pay.studentId?.let { studentMap[it] }
                val stuName = stu?.name ?: pay.payerName.ifBlank { "অজ্ঞাত শিক্ষার্থী" }
                val stuPhone = stu?.phone ?: ""
                val stuGrade = stu?.grade ?: ""
                val formattedDate = try {
                    val d = LocalDate.parse(pay.paymentDate.take(10))
                    AccountingEngine.formatDate(d, useBengaliNumerals)
                } catch (e: Exception) {
                    pay.paymentDate.take(10)
                }
                val formattedAmount = AccountingEngine.formatCurrency(pay.amountReceived, useBengaliNumerals)

                MonthlyPaymentItem(
                    payment = pay,
                    studentName = stuName,
                    studentPhone = stuPhone,
                    studentGrade = stuGrade,
                    formattedDate = formattedDate,
                    formattedAmount = formattedAmount
                )
            }

            MonthlyFinancialReport(
                yearMonth = ymStr,
                year = year,
                month = month,
                monthNameBengali = getBengaliMonthYear(year, month, useBengaliNumerals),
                monthNameEnglish = getEnglishMonthYear(year, month),
                totalCollected = totalCollected,
                totalPaymentCount = totalPaymentCount,
                payingStudentCount = payingStudentCount,
                enrolledStudentCount = enrolledStudentCount,
                payments = paymentItems,
                paymentMethodBreakdown = methodBreakdown
            )
        }

        val totalLifetime = AccountingEngine.roundMoney(validPayments.sumOf { it.amountReceived })
        val totalTransactions = validPayments.size
        val totalMonthsWithData = monthlyReports.count { it.totalPaymentCount > 0 }

        return LifetimeFinancialSummary(
            totalLifetimeCollected = totalLifetime,
            totalLifetimeTransactions = totalTransactions,
            totalMonthsWithData = totalMonthsWithData,
            totalRegisteredStudents = students.size,
            reports = monthlyReports
        )
    }

    private fun extractYearMonth(dateStr: String): String? {
        if (dateStr.length >= 7 && dateStr[4] == '-') {
            val prefix = dateStr.take(7)
            val parts = prefix.split("-")
            if (parts.size == 2 && parts[0].toIntOrNull() != null && parts[1].toIntOrNull() != null) {
                return prefix
            }
        }
        return null
    }
}
