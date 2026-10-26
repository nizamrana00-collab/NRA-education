package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.accounting.AccountingEngine
import com.example.data.entity.*
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    student: StudentEntity,
    cycles: List<BillingCycleEntity>,
    payments: List<PaymentEntity>,
    breaks: List<BreakRecordEntity>,
    auditLogs: List<AuditLogEntity>,
    useBengaliNumerals: Boolean,
    onBack: () -> Unit,
    onOpenAddPayment: () -> Unit,
    onOpenChangeFee: () -> Unit,
    onOpenRecordBreak: () -> Unit,
    onOpenChangeStatus: () -> Unit,
    onGenerateNextCycle: () -> Unit,
    onDeleteStudent: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableStateOf(0) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val tabTitles = listOf("ওভারভিউ", "ফি সাইকেল (${cycles.size})", "পেমেন্ট (${payments.size})", "বিরতি (${breaks.size})", "অডিট খতিয়ান")

    val totalPaid = payments.filter { !it.isReversed }.sumOf { it.amountReceived }
    val totalDues = cycles.sumOf { it.closingBalance }
    val totalAdvance = payments.filter { !it.isReversed }.sumOf { it.unallocatedCredit }

    var selectedCycleForDetail by remember { mutableStateOf<BillingCycleEntity?>(null) }
    var selectedPaymentForDetail by remember { mutableStateOf<PaymentEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(student.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenChangeStatus) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = "স্ট্যাটাস পরিবর্তন")
                    }
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "শিক্ষার্থী মুছুন", tint = OverdueRed)
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${student.grade} • অভিভাবক: ${student.guardianName.ifBlank { "উল্লেখ নেই" }}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (student.phone.isNotBlank()) {
                                Text(
                                    text = "মোবাইল: ${student.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusBadge(status = student.status)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("মোট পরিশোধিত", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = AccountingEngine.formatCurrency(totalPaid, useBengaliNumerals),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PaidGreen)
                            )
                        }
                        Column {
                            Text("বর্তমান মোট বকেয়া", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = AccountingEngine.formatCurrency(totalDues, useBengaliNumerals),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalDues > 0) OverdueRed else PaidGreen
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("মাসিক ফি", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = AccountingEngine.formatCurrency(student.monthlyFee, useBengaliNumerals),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    if (totalAdvance > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("অগ্রিম / অনির্ধারিত জমা:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = AccountingEngine.formatCurrency(totalAdvance, useBengaliNumerals),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = PurplePrimary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenAddPayment,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PaidGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("পেমেন্ট নিন")
                        }
                        OutlinedButton(
                            onClick = onOpenRecordBreak,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ছুটি/বিরতি")
                        }
                        OutlinedButton(
                            onClick = onOpenChangeFee,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ফি পরিবর্তন")
                        }
                    }
                }
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> OverviewTab(
                    student = student,
                    cycles = cycles,
                    useBengaliNumerals = useBengaliNumerals,
                    onGenerateNextCycle = onGenerateNextCycle,
                    onDeleteClick = { showDeleteConfirmDialog = true }
                )
                1 -> BillingCyclesTab(cycles, useBengaliNumerals, onSelectCycle = { selectedCycleForDetail = it }, onGenerateNextCycle)
                2 -> PaymentsTab(payments, useBengaliNumerals, onSelectPayment = { selectedPaymentForDetail = it })
                3 -> BreaksTab(breaks, onOpenRecordBreak)
                4 -> AuditTab(auditLogs)
            }
        }
    }

    // Delete Student Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = OverdueRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("শিক্ষার্থী মুছে ফেলবেন?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("আপনি কি নিশ্চিত যে '${student.name}'-এর সমস্ত তথ্য ও ফি রেকর্ড স্থায়ীভাবে মুছে ফেলতে চান?\n\nভুলবশত যোগ করা শিক্ষার্থীর তথ্য বাদ দিতে এটি ব্যবহার করুন। এই কাজটি আর ফিরিয়ে আনা যাবে না।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteStudent()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OverdueRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("হ্যাঁ, মুছে ফেলুন")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Detail Dialogs
    selectedCycleForDetail?.let { cycle ->
        AlertDialog(
            onDismissRequest = { selectedCycleForDetail = null },
            title = { Text("সাইকেল বিস্তারিত হিসাব") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("সাইকেল আইডি: ${cycle.id}")
                    Text("মেয়াদ: ${cycle.periodStart} হতে ${cycle.periodEnd}")
                    Text("পরিশোধের শেষ তারিখ: ${cycle.dueDate}")
                    HorizontalDivider()
                    Text("মূল ফি: ৳ ${cycle.originalFee}")
                    Text("পূর্ববর্তী বকেয়া (Opening): ৳ ${cycle.openingBalance}")
                    Text("সমন্বয় / ছাড় (Adjustment): ৳ ${cycle.adjustments}")
                    Text("মোট ধার্যকৃত চার্জ: ৳ ${cycle.totalCharge}", fontWeight = FontWeight.Bold)
                    Text("পরিশোধিত / বরাদ্দ: ৳ ${cycle.allocatedAmount}", color = PaidGreen)
                    Text("অবশিষ্ট বকেয়া (Closing): ৳ ${cycle.closingBalance}", color = if (cycle.closingBalance > 0) OverdueRed else PaidGreen, fontWeight = FontWeight.Bold)
                    Text("স্ট্যাটাস: ${cycle.status}")
                }
            },
            confirmButton = {
                Button(onClick = { selectedCycleForDetail = null }) { Text("ঠিক আছে") }
            }
        )
    }

    selectedPaymentForDetail?.let { payment ->
        PaymentDetailDialog(
            payment = payment,
            studentName = student.name,
            onDismiss = { selectedPaymentForDetail = null },
            onReversePayment = { /* ViewModel reversal handled in root */ }
        )
    }
}

@Composable
fun OverviewTab(
    student: StudentEntity,
    cycles: List<BillingCycleEntity>,
    useBengaliNumerals: Boolean,
    onGenerateNextCycle: () -> Unit,
    onDeleteClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("প্রাথমিক তথ্য ও নিয়মাবলী", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("ভর্তির তারিখ: ${student.admissionDate}")
                    Text("মাসিক বিলিং নোঙর দিন: প্রতি মাসের ${if (useBengaliNumerals) AccountingEngine.toBengaliDigits(student.billingDayAnchor.toString()) else student.billingDayAnchor} তারিখ")
                    Text("বর্তমান অবস্থা: ${student.status}")
                    if (student.address.isNotBlank()) Text("ঠিকানা: ${student.address}")
                    if (student.notes.isNotBlank()) Text("মন্তব্য: ${student.notes}")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("বিলিং সাইকেল স্বয়ংক্রিয় প্রস্তুতকরণ", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Button(
                            onClick = onGenerateNextCycle,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("পরবর্তী সাইকেল")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "শিক্ষার্থীর বিলিং নোঙর দিন অনুসারে পরবর্তী সাইকেল প্রস্তুত করা হয়। পুনরাবৃত্তিমূলক চাপ এড়াতে এটি নিরাপদ ও স্বয়ংক্রিয়।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Danger Zone: Delete Student
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ভুল এন্ট্রি মোছার অপশন",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = OverdueRed)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ভুলবশত এই শিক্ষার্থী সেভ করে থাকলে এখান থেকে স্থায়ীভাবে সমস্ত তথ্য ও ফি সাইকেল মুছে ফেলতে পারেন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.buttonColors(containerColor = OverdueRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("শিক্ষার্থীর তথ্য স্থায়ীভাবে মুছুন")
                    }
                }
            }
        }
    }
}

@Composable
fun BillingCyclesTab(
    cycles: List<BillingCycleEntity>,
    useBengaliNumerals: Boolean,
    onSelectCycle: (BillingCycleEntity) -> Unit,
    onGenerateNextCycle: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সকল ফি সাইকেলের ইতিহাস (${cycles.size}টি)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onGenerateNextCycle) {
                    Text("+ পরবর্তী সাইকেল তৈরি")
                }
            }
        }

        if (cycles.isEmpty()) {
            item {
                Text("এখনও কোনো সাইকেল নেই।")
            }
        } else {
            items(cycles) { cycle ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectCycle(cycle) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${cycle.periodStart} হতে ${cycle.periodEnd}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "শেষ তারিখ: ${cycle.dueDate} • ধার্য: ৳ ${cycle.totalCharge}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (cycle.allocatedAmount > 0) {
                                Text(
                                    text = "পরিশোধিত: ৳ ${cycle.allocatedAmount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PaidGreen
                                )
                            }
                            if (cycle.notes.contains("ছুটি")) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = InfoCyanBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "🌴 " + cycle.notes,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = InfoCyan,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            StatusBadge(status = cycle.status)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "বকেয়া: ${AccountingEngine.formatCurrency(cycle.closingBalance, useBengaliNumerals)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (cycle.closingBalance > 0) OverdueRed else PaidGreen
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentsTab(
    payments: List<PaymentEntity>,
    useBengaliNumerals: Boolean,
    onSelectPayment: (PaymentEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (payments.isEmpty()) {
            item {
                Text("এখনও কোনো পেমেন্ট রেকর্ড নেই।")
            }
        } else {
            items(payments) { payment ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectPayment(payment) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${payment.paymentDate} • ${payment.paymentMethod}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (payment.referenceId.isNotBlank()) {
                                Text(
                                    text = "রেফারেন্স: ${payment.referenceId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "+${AccountingEngine.formatCurrency(payment.amountReceived, useBengaliNumerals)}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (payment.isReversed) Color.Gray else PaidGreen
                                )
                            )
                            if (payment.isReversed) {
                                Text("বাতিলকৃত", style = MaterialTheme.typography.labelSmall, color = OverdueRed)
                            } else if (payment.unallocatedCredit > 0) {
                                Text("অগ্রিম: ৳ ${payment.unallocatedCredit}", style = MaterialTheme.typography.labelSmall, color = PurplePrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BreaksTab(
    breaks: List<BreakRecordEntity>,
    onOpenRecordBreak: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ছুটি ও বিরতির ইতিহাস", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Button(onClick = onOpenRecordBreak, shape = RoundedCornerShape(8.dp)) {
                    Text("+ ছুটি দিন")
                }
            }
        }

        if (breaks.isEmpty()) {
            item {
                Text("কোনো ছুটি বা বিরতি রেকর্ড নেই।")
            }
        } else {
            items(breaks) { b ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(b.reason.ifBlank { "ছুটি" }, fontWeight = FontWeight.Bold)
                            Text("${b.eligibleDays} দিন", color = PurplePrimary, fontWeight = FontWeight.Bold)
                        }
                        Text("শুরু: ${b.startDate}${if (b.endDate != null) " • শেষ: ${b.endDate}" else ""}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "মেয়াদ বর্ধিতকরণ সিদ্ধান্ত: ${when (b.extensionDecision) {
                                "EXTEND" -> "মেয়াদ বাড়ানো হয়েছে (প্রযুক্ত)"
                                "DO_NOT_EXTEND" -> "মেয়াদ বাড়ানো হয়নি"
                                else -> "পরে সিদ্ধান্ত নেওয়া হবে"
                            }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuditTab(auditLogs: List<AuditLogEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (auditLogs.isEmpty()) {
            item { Text("কোনো অডিট রেকর্ড নেই।") }
        } else {
            items(auditLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(log.action, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = PurplePrimary))
                            Text(log.entityType, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(log.details, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
