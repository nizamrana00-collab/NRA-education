package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.accounting.AccountingEngine
import com.example.accounting.MonthlyFinancialReport
import com.example.accounting.MonthlyPaymentItem
import com.example.accounting.MonthlyReportEngine
import com.example.data.entity.*
import com.example.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    students: List<StudentEntity>,
    batches: List<BatchEntity>,
    unpaidCycles: List<BillingCycleEntity>,
    payments: List<PaymentEntity>,
    useBengaliNumerals: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedReportType by remember { mutableStateOf(0) }
    val reportTitles = listOf(
        "মাসিক হিসাব ও ইতিহাস",
        "বকেয়া খতিয়ান",
        "শিক্ষার্থীভিত্তিক হিসাব",
        "ব্যাচভিত্তিক আয়",
        "পেমেন্ট মাধ্যম অনুপাত"
    )

    // Compute lifetime historical summary grouped strictly by paymentDate month
    val lifetimeSummary = remember(payments, students, useBengaliNumerals) {
        MonthlyReportEngine.generateHistoricalReports(payments, students, useBengaliNumerals)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "আর্থিক রিপোর্ট ও হিসাবের ইতিহাস",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "প্রকৃত আদায়ের তারিখভিত্তিক পূর্ণাঙ্গ মাসিক বিবরণী ও আজীবন ইতিহাস",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Permanent Data Preservation Notice Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SoftLavender,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "সুরক্ষিত",
                    tint = DeepPurple,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "স্থায়ী আজীবন রেকর্ড • অ্যাপ ইনস্টল থেকে এ যাবৎকালের কোনো ডাটা মুছে যাবে না",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = DeepPurple
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Report Type Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedReportType,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            reportTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedReportType == index,
                    onClick = { selectedReportType = index },
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selectedReportType == index) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedReportType) {
            0 -> MonthlyHistoryArchiveReport(
                lifetimeSummary = lifetimeSummary,
                useBengaliNumerals = useBengaliNumerals
            )
            1 -> DueLedgerReport(unpaidCycles, students, useBengaliNumerals)
            2 -> StudentLedgerReport(students, unpaidCycles, payments, useBengaliNumerals)
            3 -> BatchRevenueReport(batches, students, unpaidCycles, payments, useBengaliNumerals)
            4 -> PaymentMethodReport(payments, useBengaliNumerals)
        }
    }
}

/**
 * Tab 0: Comprehensive Monthly Historical Archive
 * Grouped strictly by paymentDate month, displaying:
 * - Total money received in each month
 * - Student counts (paying & enrolled)
 * - Complete historical archive with Search & Year Filters
 * - Detail sheet showing all payment receipts for that month
 */
@Composable
fun MonthlyHistoryArchiveReport(
    lifetimeSummary: com.example.accounting.LifetimeFinancialSummary,
    useBengaliNumerals: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedYearFilter by remember { mutableStateOf("ALL") }
    var selectedMonthForDetails by remember { mutableStateOf<MonthlyFinancialReport?>(null) }

    // Collect all available years
    val availableYears = remember(lifetimeSummary.reports) {
        lifetimeSummary.reports.map { it.year.toString() }.distinct().sortedDescending()
    }

    // Filter reports based on search query and year filter
    val filteredReports = remember(lifetimeSummary.reports, searchQuery, selectedYearFilter) {
        lifetimeSummary.reports.filter { report ->
            val matchesYear = selectedYearFilter == "ALL" || report.year.toString() == selectedYearFilter
            val queryClean = searchQuery.trim().lowercase()

            val matchesSearch = if (queryClean.isEmpty()) {
                true
            } else {
                report.monthNameBengali.lowercase().contains(queryClean) ||
                        report.monthNameEnglish.lowercase().contains(queryClean) ||
                        report.yearMonth.contains(queryClean) ||
                        report.payments.any { it.studentName.lowercase().contains(queryClean) }
            }

            matchesYear && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Lifetime Header KPI Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(listOf(DeepPurple, VibrantPurple, RoyalIndigo))
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "আজীবন মোট আদায় (Lifetime Total)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = AccountingEngine.formatCurrency(
                                        lifetimeSummary.totalLifetimeCollected,
                                        useBengaliNumerals
                                    ),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp
                                    ),
                                    color = Color.White
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("মোট লেনদেন রসিদ", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                Text(
                                    text = if (useBengaliNumerals)
                                        "${AccountingEngine.toBengaliDigits(lifetimeSummary.totalLifetimeTransactions.toString())} টি"
                                    else
                                        "${lifetimeSummary.totalLifetimeTransactions} টি",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            Column {
                                Text("সক্রিয় আদায়ের মাস", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                Text(
                                    text = if (useBengaliNumerals)
                                        "${AccountingEngine.toBengaliDigits(lifetimeSummary.totalMonthsWithData.toString())} টি"
                                    else
                                        "${lifetimeSummary.totalMonthsWithData} টি",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            Column {
                                Text("নিবন্ধিত শিক্ষার্থী", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                Text(
                                    text = if (useBengaliNumerals)
                                        "${AccountingEngine.toBengaliDigits(lifetimeSummary.totalRegisteredStudents.toString())} জন"
                                    else
                                        "${lifetimeSummary.totalRegisteredStudents} জন",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search Bar & Filter Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_search_input"),
                    placeholder = {
                        Text(
                            text = "মাস (যেমন: মার্চ, 2026) বা শিক্ষার্থীর নাম খুঁজুন...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "অনুসন্ধান", tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "মুছুন")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Year filter chips row
                if (availableYears.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedYearFilter == "ALL",
                                onClick = { selectedYearFilter = "ALL" },
                                label = { Text("সকল বছর") },
                                leadingIcon = if (selectedYearFilter == "ALL") {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }

                        items(availableYears) { year ->
                            val yearLabel = if (useBengaliNumerals) AccountingEngine.toBengaliDigits(year) else year
                            FilterChip(
                                selected = selectedYearFilter == year,
                                onClick = { selectedYearFilter = year },
                                label = { Text(yearLabel) },
                                leadingIcon = if (selectedYearFilter == year) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "মাসভিত্তিক আয় ও শিক্ষার্থীর খতিয়ান (${filteredReports.size}টি মাস)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Monthly Reports List
        if (filteredReports.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "কোনো মাসের তথ্য পাওয়া যায়নি",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "ভিন্ন কোনো মাস বা শিক্ষার্থীর নাম দিয়ে অনুসন্ধান করুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredReports, key = { it.yearMonth }) { report ->
                MonthlyReportCard(
                    report = report,
                    useBengaliNumerals = useBengaliNumerals,
                    searchQuery = searchQuery,
                    onViewDetails = { selectedMonthForDetails = report }
                )
            }
        }
    }

    // Detail Dialog for selected month
    selectedMonthForDetails?.let { report ->
        MonthlyDetailDialog(
            report = report,
            useBengaliNumerals = useBengaliNumerals,
            onDismiss = { selectedMonthForDetails = null }
        )
    }
}

/**
 * Individual Card representing one month's complete financial report.
 */
@Composable
fun MonthlyReportCard(
    report: MonthlyFinancialReport,
    useBengaliNumerals: Boolean,
    searchQuery: String,
    onViewDetails: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val isCurrentMonth = report.year == today.year && report.month == today.monthValue

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("month_card_${report.yearMonth}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Month Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrentMonth) DeepPurple else SoftLavender,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = if (isCurrentMonth) Color.White else DeepPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = report.monthNameBengali,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (isCurrentMonth) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PaidGreenBg
                                ) {
                                    Text(
                                        text = "চলতি মাস",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PaidGreen
                                    )
                                }
                            }
                        }
                        Text(
                            text = report.monthNameEnglish,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Month's Total Money Collected
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "মোট আদায়",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AccountingEngine.formatCurrency(report.totalCollected, useBengaliNumerals),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (report.totalCollected > 0) PaidGreen else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Student Count & Transactions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Paying Students Count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = RoyalIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (useBengaliNumerals)
                            "পরিশোধকারী: ${AccountingEngine.toBengaliDigits(report.payingStudentCount.toString())} জন"
                        else
                            "পরিশোধকারী: ${report.payingStudentCount} জন",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }

                // Enrolled Student Count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = DeepPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (useBengaliNumerals)
                            "মোট শিক্ষার্থী: ${AccountingEngine.toBengaliDigits(report.enrolledStudentCount.toString())} জন"
                        else
                            "মোট শিক্ষার্থী: ${report.enrolledStudentCount} জন",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }

                // Receipt/Payment Count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = PaidGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (useBengaliNumerals)
                            "${AccountingEngine.toBengaliDigits(report.totalPaymentCount.toString())}টি রশিদ"
                        else
                            "${report.totalPaymentCount}টি রশিদ",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }

            // Payment Methods Breakdown (if any)
            if (report.paymentMethodBreakdown.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    report.paymentMethodBreakdown.forEach { (method, amount) ->
                        val (label, bg, fg) = when (method) {
                            "CASH" -> Triple("নগদ", PaidGreenBg, PaidGreen)
                            "BKASH" -> Triple("বিকাশ", Color(0xFFFFEBEE), Color(0xFFD81B60))
                            "NAGAD" -> Triple("নগদ পে", DueOrangeBg, DueOrange)
                            "BANK_TRANSFER" -> Triple("ব্যাংক", InfoCyanBg, InfoCyan)
                            else -> Triple(method, SoftLavender, DeepPurple)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bg
                        ) {
                            Text(
                                text = "$label: ${AccountingEngine.formatCurrency(amount, useBengaliNumerals)}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = fg
                            )
                        }
                    }
                }
            }

            // If user searched for a student, show which payments belong to them
            if (searchQuery.isNotBlank()) {
                val matchingStudentPayments = report.payments.filter {
                    it.studentName.lowercase().contains(searchQuery.trim().lowercase())
                }
                if (matchingStudentPayments.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LavenderCard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "এই মাসে পাওয়া পেমেন্ট:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = DeepPurple
                            )
                            matchingStudentPayments.forEach { pay ->
                                Text(
                                    text = "• ${pay.studentName}: ${pay.formattedAmount} (${pay.formattedDate})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // View Itemized Payment Detail Button
            Button(
                onClick = onViewDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_month_details_${report.yearMonth}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (report.totalPaymentCount > 0)
                        "বিস্তারিত রসিদ ও শিক্ষার্থী তালিকা (${report.totalPaymentCount}টি পেমেন্ট)"
                    else
                        "মাসের হিসাব ও শিক্ষার্থীদের তথ্য দেখুন",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * Detailed Dialog/Sheet showing every individual payment received in a specific month,
 * with search within that month, student name, date, receipt, method, and notes.
 */
@Composable
fun MonthlyDetailDialog(
    report: MonthlyFinancialReport,
    useBengaliNumerals: Boolean,
    onDismiss: () -> Unit
) {
    var itemSearchQuery by remember { mutableStateOf("") }

    val filteredPayments = remember(report.payments, itemSearchQuery) {
        if (itemSearchQuery.isBlank()) {
            report.payments
        } else {
            val q = itemSearchQuery.trim().lowercase()
            report.payments.filter {
                it.studentName.lowercase().contains(q) ||
                        it.studentPhone.contains(q) ||
                        it.payment.referenceId.lowercase().contains(q)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${report.monthNameBengali} — বিস্তারিত আদায়",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        )
                        Text(
                            text = "মোট আদায়: ${AccountingEngine.formatCurrency(report.totalCollected, useBengaliNumerals)} • ${report.payingStudentCount} জন শিক্ষার্থী",
                            style = MaterialTheme.typography.bodySmall,
                            color = PaidGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search inside month
                if (report.payments.size > 2) {
                    OutlinedTextField(
                        value = itemSearchQuery,
                        onValueChange = { itemSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("শিক্ষার্থীর নাম বা ফোন দিয়ে খুঁজুন...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (itemSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { itemSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // List of Payments
                if (filteredPayments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (report.payments.isEmpty()) "এই মাসে কোনো পেমেন্ট জমা হয়নি" else "অনুসন্ধানে কোনো পেমেন্ট পাওয়া যায়নি",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPayments, key = { it.payment.id }) { item ->
                            PaymentReceiptItemCard(
                                item = item,
                                useBengaliNumerals = useBengaliNumerals
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("বন্ধ করুন")
                }
            }
        }
    }
}

/**
 * Itemized Receipt Card for a single payment in the monthly breakdown.
 */
@Composable
fun PaymentReceiptItemCard(
    item: MonthlyPaymentItem,
    useBengaliNumerals: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.studentName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = item.formattedAmount,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PaidGreen)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment date
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "আদায়ের তারিখ: ${item.formattedDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Payment method badge
                val methodLabel = when (item.payment.paymentMethod) {
                    "CASH" -> "নগদ"
                    "BKASH" -> "বিকাশ"
                    "NAGAD" -> "নগদ পে"
                    "BANK_TRANSFER" -> "ব্যাংক"
                    else -> item.payment.paymentMethod
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SoftLavender
                ) {
                    Text(
                        text = methodLabel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = DeepPurple
                    )
                }
            }

            if (item.studentPhone.isNotBlank() || item.studentGrade.isNotBlank()) {
                Text(
                    text = "${item.studentGrade} ${if (item.studentPhone.isNotBlank()) "• ${item.studentPhone}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.payment.referenceId.isNotBlank()) {
                Text(
                    text = "রশিদ / ট্রানজ্যাকশন আইডি: ${item.payment.referenceId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (item.payment.notes.isNotBlank()) {
                Text(
                    text = "মন্তব্য: ${item.payment.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DueLedgerReport(
    unpaidCycles: List<BillingCycleEntity>,
    students: List<StudentEntity>,
    useBengaliNumerals: Boolean
) {
    val studentMap = remember(students) { students.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Text(
                text = "বকেয়া ফি সাইকেল খতিয়ান (${unpaidCycles.size}টি অপরিশোধিত)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(unpaidCycles) { cycle ->
            val stu = cycle.studentId?.let { studentMap[it] }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stu?.name ?: "যৌথ বিলিং", fontWeight = FontWeight.Bold)
                        Text("${cycle.periodStart} হতে ${cycle.periodEnd} (পরিশোধের শেষ: ${cycle.dueDate})", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = AccountingEngine.formatCurrency(cycle.closingBalance, useBengaliNumerals),
                        fontWeight = FontWeight.Bold,
                        color = OverdueRed
                    )
                }
            }
        }
    }
}

@Composable
fun StudentLedgerReport(
    students: List<StudentEntity>,
    cycles: List<BillingCycleEntity>,
    payments: List<PaymentEntity>,
    useBengaliNumerals: Boolean
) {
    val duesByStudent = remember(cycles) {
        cycles.groupBy { it.studentId }.mapValues { (_, cl) -> cl.sumOf { it.closingBalance } }
    }
    val paidByStudent = remember(payments) {
        payments.filter { !it.isReversed }.groupBy { it.studentId }.mapValues { (_, pl) -> pl.sumOf { it.amountReceived } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        items(students) { stu ->
            val due = duesByStudent[stu.id] ?: 0.0
            val paid = paidByStudent[stu.id] ?: 0.0

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stu.name, fontWeight = FontWeight.Bold)
                        Text("${stu.grade} • মাসিক ফি: ৳ ${stu.monthlyFee}", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("পরিশোধ: ${AccountingEngine.formatCurrency(paid, useBengaliNumerals)}", style = MaterialTheme.typography.labelSmall, color = PaidGreen)
                        Text(
                            text = "বকেয়া: ${AccountingEngine.formatCurrency(due, useBengaliNumerals)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = if (due > 0) OverdueRed else PaidGreen)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BatchRevenueReport(
    batches: List<BatchEntity>,
    students: List<StudentEntity>,
    cycles: List<BillingCycleEntity>,
    payments: List<PaymentEntity>,
    useBengaliNumerals: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        items(batches) { batch ->
            val batchStudents = students.filter { it.batchId == batch.id }
            val studentIds = batchStudents.map { it.id }.toSet()
            val batchDues = cycles.filter { it.studentId in studentIds }.sumOf { it.closingBalance }
            val batchPaid = payments.filter { !it.isReversed && it.studentId in studentIds }.sumOf { it.amountReceived }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(batch.name, fontWeight = FontWeight.Bold)
                        Text("${batchStudents.size} জন", color = PurplePrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("মোট আদায়: ${AccountingEngine.formatCurrency(batchPaid, useBengaliNumerals)}", style = MaterialTheme.typography.bodySmall, color = PaidGreen)
                        Text("মোট বকেয়া: ${AccountingEngine.formatCurrency(batchDues, useBengaliNumerals)}", style = MaterialTheme.typography.bodySmall, color = if (batchDues > 0) OverdueRed else PaidGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentMethodReport(
    payments: List<PaymentEntity>,
    useBengaliNumerals: Boolean
) {
    val validPayments = payments.filter { !it.isReversed }
    val grouped = validPayments.groupBy { it.paymentMethod }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Text("পেমেন্ট পদ্ধতি অনুযায়ী আদায়", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }

        items(grouped.entries.toList()) { (method, list) ->
            val total = list.sumOf { it.amountReceived }
            val label = when (method) {
                "CASH" -> "নগদ (Cash)"
                "BKASH" -> "বিকাশ (bKash)"
                "NAGAD" -> "নগদ পে (Nagad)"
                "BANK_TRANSFER" -> "ব্যাংক ট্রান্সফার"
                else -> method
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(label, fontWeight = FontWeight.Bold)
                        Text("${list.size}টি লেনদেন", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = AccountingEngine.formatCurrency(total, useBengaliNumerals),
                        fontWeight = FontWeight.Bold,
                        color = PaidGreen
                    )
                }
            }
        }
    }
}
